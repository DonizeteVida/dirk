package visitor.inject

import Factory
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.writeTo
import visitor.FunctionVisitor
import information.FactoryInfo
import information.ParameterInfo
import information.Root
import visitor.ClassVisitor

class FactoryGeneratorVisitor(
    private val codeGenerator: CodeGenerator,
    private val kspLogger: KSPLogger,
    private val root: Root,
    private val classVisitor: ClassVisitor,
    private val functionVisitor: FunctionVisitor
) : KSVisitorVoid() {
    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
        val factoryInfo = FactoryInfo()

        classDeclaration.accept(classVisitor, factoryInfo.parameterInfo)
        classDeclaration.primaryConstructor?.accept(functionVisitor, factoryInfo.functionInfo)

        root += factoryInfo

        val factoryName = "${factoryInfo.parameterInfo.name}_Factory"
        val factory = Factory::class.asClassName()

        val fileSpec = FileSpec
            .builder(factoryInfo.parameterInfo.packageName, factoryName)
            .apply {
                addImport(packageName, name)
                factoryInfo.functionInfo.inParameterInfoList.values.forEach { (packageName, name) ->
                    addImport(packageName, name)
                }

                addType(
                    TypeSpec.classBuilder(factoryName)
                        .apply {
                            primaryConstructor(
                                FunSpec.constructorBuilder().apply {
                                    factoryInfo.functionInfo.inParameterInfoList.values.forEach {
                                        val lowercase = it.name.lowercase()
                                        val parameter = factory.parameterizedBy(it.asClassName())
                                        addParameter(
                                            lowercase,
                                            parameter,
                                        )
                                        addProperty(
                                            PropertySpec.builder(
                                                lowercase,
                                                parameter,
                                                KModifier.PRIVATE
                                            ).initializer(
                                                lowercase
                                            ).build()
                                        )
                                    }
                                }.build()
                            )
                            addSuperinterface(
                                factory.parameterizedBy(factoryInfo.parameterInfo.asClassName())
                            )
                            addFunction(
                                FunSpec.builder("invoke").apply {
                                    addModifiers(
                                        KModifier.OVERRIDE,
                                        KModifier.OPERATOR
                                    )
                                    addStatement(
                                        StringBuilder().apply {
                                            append("return ${factoryInfo.parameterInfo.name}(")
                                            val dependencies = factoryInfo
                                                .functionInfo
                                                .inParameterInfoList
                                                .values
                                                .joinToString(", ") {
                                                    "${it.name.lowercase()}()"
                                                }
                                            append(dependencies)
                                            append(")")
                                        }.toString()
                                    )
                                }.build()
                            )
                        }.build()
                )
            }.build()

        fileSpec.writeTo(
            codeGenerator,
            Dependencies(true, classDeclaration.containingFile!!)
        )
    }
}