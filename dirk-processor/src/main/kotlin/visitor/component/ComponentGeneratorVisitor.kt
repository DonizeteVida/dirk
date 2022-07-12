package visitor.component

import Factory
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.writeTo
import information.*
import visitor.FunctionVisitor
import visitor.ClassVisitor

class ComponentGeneratorVisitor(
    private val codeGenerator: CodeGenerator,
    private val kspLogger: KSPLogger,
    private val root: Root,
    private val classVisitor: ClassVisitor,
    private val functionVisitor: FunctionVisitor
) : KSVisitorVoid() {
    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
        val componentInfo = ComponentInfo()

        classDeclaration.accept(classVisitor, componentInfo.parameterInfo)

        root += componentInfo

        val componentName = "Dirk${componentInfo.parameterInfo.name}"

        classDeclaration.declarations.filterIsInstance<KSFunctionDeclaration>().forEach {
            val functionInfo = FunctionInfo()
            it.accept(functionVisitor, functionInfo)
            componentInfo.functionInfoList += functionInfo
        }

        val factoryClass = Factory::class.asClassName()

        val fileSpec = FileSpec.builder(componentInfo.parameterInfo.packageName, componentName).apply {
            addType(
                TypeSpec.classBuilder(componentName).apply {
                    //implements interface
                    addSuperinterface(
                        componentInfo.parameterInfo.asClassName()
                    )

                    //instantiate factories
                    orderFactories().map {
                        val factory = root.factories[it]!!
                        val name = factory.parameterInfo.name
                        PropertySpec.builder(
                            name.lowercase(),
                            factoryClass.parameterizedBy(factory.parameterInfo.asClassName()),
                            KModifier.PRIVATE
                        ).apply {
                            val str = StringBuilder().apply {
                                append("${name}_Factory(")
                                val factoryParameters = factory
                                    .functionInfo
                                    .inParameterInfoList
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
                        val (packageName, name) = it.outParameterInfo
                        addImport(packageName, name)
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
        val fullName = factory.parameterInfo.fullName
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
        factory.functionInfo.inParameterInfoList.apply {
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