package visitor.component

import Names.FACTORY_BY
import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.writeTo
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
        //here will get informations about who is annotated by @Module
        //what should we know?
        //we have to get knowledge about functions who it is capable to provides
        if (!classDeclaration.isAbstract()) {
            kspLogger.error("@Module must be used on abstract classes: ${classDeclaration.simpleName.getShortName()}")
            return
        }

        val componentInfo = ComponentInfo()

        //As every place in code, we will get knowledge about the
        //class which is annotated by @Module, because we will
        //have to implement it
        classDeclaration.accept(classVisitor, componentInfo.classInfo)

        root += componentInfo

        val componentName = "Dirk${componentInfo.classInfo.name}"

        //Here we will get knowledge about all functions inside this Module
        //its name, its parameters and its return type
        //function visitor will call ClassVisitor for
        //each parameter and return type
        classDeclaration.declarations.filterIsInstance<KSFunctionDeclaration>().forEach {
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
        val fileSpec = FileSpec.builder(componentInfo.classInfo.packageName, componentName).apply {
            addType(
                TypeSpec.classBuilder(componentName).apply {
                    //implements interface
                    addSuperinterface(
                        componentInfo.classInfo.asClassName()
                    )

                    //instantiate factories
                    orderFactories().map {
                        val factory = root.factories[it]!!
                        val name = factory.classInfo.name
                        PropertySpec.builder(
                            name.lowercase(),
                            FACTORY_BY(factory.classInfo.asClassName()),
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
        val fullName = factory.classInfo.fullName
        if (fullName in toCreate) return
        if (fullName in history) {
            val str = StringBuilder().apply {
                history.forEach {
                    append("$it -> ")
                }
                append(fullName)
            }.toString()
            kspLogger.error("Circular dependency: $str")
            return
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
                            return
                        }
                        resolveFactory(dependsOn, factories, history, toCreate)
                    }
                }
                toCreate += fullName
                history -= fullName
            }
        }
    }
}