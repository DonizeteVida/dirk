package visitor.component

import asFactoryParam
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.writeTo
import getDeclaredClasses
import information.ComponentInfo
import information.FactoryInfo
import information.FunctionInfo
import information.Root
import visitor.ClassVisitor
import visitor.FunctionVisitor

class ComponentGeneratorVisitor(
    private val codeGenerator: CodeGenerator,
    private val kspLogger: KSPLogger,
    private val root: Root,
    private val classVisitor: ClassVisitor,
    private val functionVisitor: FunctionVisitor
) : KSVisitorVoid() {
    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
        //here will get informations about who is annotated by @Component
        //what should we know?
        //we have to get knowledge about functions who it is capable to provides
        if (!classDeclaration.isAbstract()) {
            kspLogger.error("@Component must be used on abstract classes: ${classDeclaration.simpleName.getShortName()}")
        }

        val componentInfo = ComponentInfo()

        //As every place in code, we will get knowledge about the
        //class which is annotated by @Component, because we will
        //have to implement it
        classDeclaration.accept(classVisitor, componentInfo.classInfo)

        root += componentInfo

        //We're verifying if it has a class named 'Builder'
        //As we must implement it to pass things that cannot be instantiated by
        //@Inject or @Module
        //such as Context in Android
        //because it's a lifecycle instance
        val componentBuilderInfo = componentInfo.componentBuilderInfo
        classDeclaration.getDeclaredClasses().apply {
            if (count() > 1) {
                kspLogger.error(
                    """
                    We must have only one class inside @Component class
                    Please revise: ${joinToString(", ") { declaration -> declaration.simpleName.getShortName() }}
                    """
                )
            }
            firstOrNull()?.apply {
                if (simpleName.getShortName() != "Builder") {
                    kspLogger.error(
                        """
                    The class inside @Component must be named "Builder"
                    Please rename the "${simpleName.getShortName()}"
                    """
                    )
                }
                accept(classVisitor, componentBuilderInfo.classInfo)
                getDeclaredFunctions().forEach {
                    val functionInfo = FunctionInfo()
                    it.accept(functionVisitor, functionInfo)
                    componentBuilderInfo += functionInfo
                }
            }
        }

        //Later it we will verify if builder is correct
        //What is correct in this case?
        //A builder must have a build function which returns the @Component's class itself
        //and all other functions must return the builder itself
        componentBuilderInfo.functionInfoList.apply {
            if (isNotEmpty()) {
                //here is our deal: if it's empty, even if it has a
                //Builder class, we will ignore it, otherwise, we will
                //implement the builder class
                //let's verify
                val build = get("build")
                if (build == null) {
                    kspLogger.error("A Builder must have a build function")
                    return
                }
                //let's verify the return type
                val parentClassInfo = componentInfo.classInfo
                val functionOutClassInfo = build.outClassInfo
                if (parentClassInfo != functionOutClassInfo) {
                    kspLogger.error("A Builder build function must return it's parent type")
                }
                //If we get here, at least we have a build function which
                //return it's @Component class correctly
                //let's verify the others
                val builderClassInfo = componentBuilderInfo.classInfo
                forEach { (key, value) ->
                    //ignore build, because it is already validated
                    if (key == "build") return@forEach
                    //we expect all functions return the builder itself
                    if (value.outClassInfo != builderClassInfo) {
                        kspLogger.error(
                            """
                            All builder's functions must return the builder itself.
                            Please revise the "${value.name}" function.
                            """
                        )
                    }
                }
            }
        }

        val componentName = "Dirk${componentInfo.classInfo.name}"

        //Here we will get knowledge about all functions inside this Module
        //its name, its parameters and its return type
        //function visitor will call ClassVisitor for
        //each parameter and return type
        classDeclaration.getDeclaredFunctions().forEach {
            val functionInfo = FunctionInfo()
            it.accept(functionVisitor, functionInfo)
            componentInfo.functionInfoList += functionInfo
        }

        //Here is where we will build the class itself
        //we are supposing a lot of things at the moment
        //there is a lot to be verified, such as name conflicts
        //binds logic e so on
        //for know its okay
        //must be improved
        val fileSpec = FileSpec.builder(componentInfo.classInfo.pack, componentName).apply {
            addType(
                TypeSpec.classBuilder(componentName).apply {
                    //implements interface
                    addSuperinterface(
                        componentInfo.classInfo.className
                    )

                    //instantiate factories
                    orderFactories().map {
                        val factory = root.factories[it]!!
                        val name = factory.classInfo.name
                        PropertySpec.builder(
                            name.lowercase(),
                            factory.classInfo.className.asFactoryParam(),
                            KModifier.PRIVATE
                        ).apply {
                            val str = StringBuilder().apply {
                                append("${name}_Factory(")
                                val factoryParameters = factory
                                    .functionInfo
                                    .inClassInfoList
                                    .values
                                    .joinToString(", ") { parameterInfo ->
                                        parameterInfo.name.lowercase()
                                    }
                                append(factoryParameters)
                                append(")")
                            }.toString()
                            initializer(str)
                        }.build()
                    }.also(::addProperties)

                    //implement described functions on @Component
                    //using previous instantiated factories
                    componentInfo.functionInfoList.forEach {
                        val (packageName, name) = it.outClassInfo
                        addImport(packageName, name, "${name}_Factory")
                        addFunction(
                            FunSpec.builder(it.name).apply {
                                addModifiers(
                                    KModifier.OVERRIDE
                                )
                                addStatement(
                                    StringBuilder().apply {
                                        append("return ")
                                        append(name.lowercase())
                                        append("()")
                                    }.toString()
                                )
                            }.build()
                        )
                    }
                }.build()
            )
        }.build()

        fileSpec.writeTo(
            codeGenerator,
            Dependencies(true, classDeclaration.containingFile!!)
        )
    }

    //it deserves a separated function because it'll be quite complex
    private fun orderFactories(): Set<String> {
        //to detect circular dependencies
        val history = linkedSetOf<String>()

        //to know if it's already created
        //linked to preserve order
        //we are here only to get the correct order
        val toCreate = linkedSetOf<String>()

        val factories = root.factories

        factories.forEach { (_, factory) ->
            resolveFactory(factory, factories, history, toCreate)
        }

        return toCreate
    }

    private fun resolveFactory(
        factory: FactoryInfo,
        factories: HashMap<String, FactoryInfo>,
        history: MutableSet<String>,
        toCreate: MutableSet<String>
    ) {
        val fullName = factory.classInfo.qualified
        if (fullName in toCreate) return
        if (fullName in history) {
            val str = StringBuilder().apply {
                history.forEach {
                    append("$it -> ")
                }
                append(fullName)
            }.toString()
            kspLogger.error("Circular dependency: $str")
        }
        factory.functionInfo.inClassInfoList.apply {
            if (isEmpty()) {
                //it has no dependencies
                toCreate += fullName
            } else {
                history += fullName
                forEach { (key, _) ->
                    if (key !in toCreate) {
                        val dependsOn = factories[key]
                        if (dependsOn == null) {
                            kspLogger.error("Factory $key not found")
                        } else {
                            resolveFactory(dependsOn, factories, history, toCreate)
                        }
                    }
                }
                toCreate += fullName
                history -= fullName
            }
        }
    }
}