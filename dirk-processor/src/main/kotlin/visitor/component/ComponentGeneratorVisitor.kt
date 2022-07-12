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

        val factory = Factory::class.asClassName()

        val fileSpec = FileSpec.builder(componentInfo.parameterInfo.packageName, componentName).apply {
            addType(
                TypeSpec.classBuilder(componentName).apply {
                    addSuperinterface(
                        componentInfo.parameterInfo.asClassName()
                    )
                    val factories = root.factories
                    val resolvedFactories = orderFactories()
                    resolvedFactories.forEach {
                        val resolvedFactory = factories[it]!!
                        addProperty(
                            PropertySpec.builder(
                                resolvedFactory.parameterInfo.name.lowercase(),
                                factory.parameterizedBy(resolvedFactory.parameterInfo.asClassName())
                            ).apply {
                                val str = StringBuilder().apply {
                                    append("${resolvedFactory.parameterInfo.name}_Factory(")
                                    val parameters = resolvedFactory.functionInfo.inParameterInfoList.values.joinToString(", ") {
                                        it.name.lowercase()
                                    }
                                    append(parameters)
                                    append(")")
                                }.toString()
                                initializer(str)
                            }.build()
                        )
                    }
                    componentInfo.functionInfoList.forEach {
                        it.outParameterInfo?.let { (packageName, name) ->
                            addImport(packageName, name)
                        }
                        addFunction(
                            FunSpec.builder(it.name).apply {
                                addModifiers(
                                    KModifier.OVERRIDE
                                )
                                returns(
                                    TypeVariableName.invoke(it.outParameterInfo?.name ?: "")
                                )
                                addStatement(
                                    StringBuilder().apply {
                                        append("return ")
                                        if (it.outParameterInfo == null) {
                                            append("Unit")
                                        } else {
                                            append(it.outParameterInfo!!.name.lowercase())
                                            append("()")
                                        }
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
        if (factory.functionInfo.inParameterInfoList.isEmpty()) {
            //it has no dependencies
            toCreate += fullName
            kspLogger.warn(fullName)
            return
        }
        factory.functionInfo.inParameterInfoList.forEach {(key, _) ->
            if (key in toCreate) return@forEach
            history += fullName
            val dependsOn = factories[key]
            if (dependsOn == null) {
                kspLogger.error("Factory $key not found")
                return
            }
            resolveFactory(dependsOn, factories, history, toCreate)
            toCreate += fullName
            history -= fullName
        }
    }
}