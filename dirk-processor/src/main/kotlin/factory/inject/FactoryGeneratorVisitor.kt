package factory.inject

import Factory
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.writeTo
import factory.FunctionVisitor
import information.FactoryInfo
import information.ParameterInfo
import information.Root

class FactoryGeneratorVisitor(
    private val codeGenerator: CodeGenerator,
    private val kspLogger: KSPLogger,
    private val root: Root
) : KSVisitorVoid() {
    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
        val name = classDeclaration.simpleName.getShortName()
        val packageName = classDeclaration.packageName.asString()
        val fullName = "$packageName.$name"

        val factoryInfo = FactoryInfo(
            parameterInfo = ParameterInfo(
                packageName = packageName,
                name = name,
                fullName = fullName
            )
        )

        root += factoryInfo

        classDeclaration.primaryConstructor?.accept(FunctionVisitor(kspLogger), factoryInfo.functionInfo)

        val factoryName = "${name}_Factory"

        val fileSpec = FileSpec
            .builder(packageName, factoryName)
            .apply {
                addImport(packageName, name)
                factoryInfo.functionInfo.inParameterInfoList.forEach { (packageName, name) ->
                    addImport(packageName, name)
                }

                addType(
                    TypeSpec.classBuilder(factoryName)
                        .apply {
                            primaryConstructor(
                                FunSpec.constructorBuilder().apply {
                                    factoryInfo.functionInfo.inParameterInfoList.forEach {
                                        val lowercase = it.name.lowercase()
                                        val parameterizedFactory =
                                            Factory::class
                                                .asClassName()
                                                .parameterizedBy(it.asClassName())
                                        addParameter(
                                            lowercase,
                                            parameterizedFactory,
                                        )
                                        addProperty(
                                            PropertySpec.builder(
                                                lowercase,
                                                parameterizedFactory,
                                                KModifier.PRIVATE
                                            ).initializer(
                                                lowercase
                                            ).build()
                                        )
                                    }
                                }.build()
                            )
                            addSuperinterface(
                                Factory::class
                                    .asClassName()
                                    .parameterizedBy(factoryInfo.parameterInfo.asClassName())
                            )
                            addFunction(
                                FunSpec.builder("invoke").apply {
                                    addModifiers(
                                        KModifier.OVERRIDE,
                                        KModifier.OPERATOR
                                    )
//                                    returns(
//                                        TypeVariableName.invoke(factoryInfo.name)
//                                    )
                                    addStatement(
                                        StringBuilder().apply {
                                            append("return ${factoryInfo.parameterInfo.name}(")
                                            val dependencies = factoryInfo
                                                .functionInfo
                                                .inParameterInfoList
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