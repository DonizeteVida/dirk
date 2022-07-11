package visitor

import Factory
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.writeTo
import information.Dependency
import information.Root
import information.asClassName

class ClassVisitor(
    private val codeGenerator: CodeGenerator,
    private val kspLogger: KSPLogger,
    private val root: Root
) : KSVisitorVoid() {
    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
        val name = classDeclaration.simpleName.getShortName()
        val packageName = classDeclaration.packageName.asString()
        val fullName = "$packageName.$name"

        kspLogger.warn("Full name: $fullName")

        if (fullName in root) {
            kspLogger.warn("$fullName is already mapped")
            return
        }

        val dependency = Dependency(
            packageName = packageName,
            name = name,
            fullName = fullName
        )

        root += dependency

        classDeclaration.primaryConstructor?.accept(ConstructorVisitor(kspLogger), dependency)

        val factoryName = "${name}_Factory"

        val fileSpec = FileSpec
            .builder(packageName, factoryName)
            .apply {
                addFileComment(dependency.toString())

                addImport(packageName, name)
                dependency.dependencies.forEach {
                    addImport(it.packageName, it.name)
                }

                addType(
                    TypeSpec.classBuilder(factoryName)
                        .apply {
                            primaryConstructor(
                                FunSpec.constructorBuilder().apply {
                                    dependency.dependencies.forEach {
                                        val lowercase = it.name.lowercase()
                                        addParameter(
                                            lowercase,
                                            Factory::class
                                                .asClassName()
                                                .parameterizedBy(it.asClassName()),
                                        )
                                        addProperty(
                                            PropertySpec.builder(
                                                lowercase,
                                                Factory::class
                                                    .asClassName()
                                                    .parameterizedBy(it.asClassName()),
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
                                    .parameterizedBy(dependency.asClassName())
                            )
                            addFunction(
                                FunSpec.builder("invoke").apply {
                                    addModifiers(
                                        KModifier.OVERRIDE,
                                        KModifier.OPERATOR
                                    )
                                    returns(
                                        TypeVariableName.invoke(dependency.name)
                                    )
                                    addStatement(
                                        StringBuilder().apply {
                                            append("return ${dependency.name}(")
                                            val dependencies = dependency.dependencies.joinToString(", ") {
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