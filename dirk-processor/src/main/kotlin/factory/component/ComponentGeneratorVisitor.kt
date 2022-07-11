package factory.component

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.writeTo
import factory.FunctionVisitor
import information.ComponentInfo
import information.FunctionInfo
import information.ParameterInfo
import information.Root

class ComponentGeneratorVisitor(
    private val codeGenerator: CodeGenerator,
    private val kspLogger: KSPLogger,
    private val root: Root
) : KSVisitorVoid() {
    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
        val packageName = classDeclaration.packageName.getShortName()
        val name = classDeclaration.simpleName.getShortName()
        val fullName = "$packageName.$name"
        val componentName = "Dirk${name}"

        val componentInfo = ComponentInfo(
            parameterInfo = ParameterInfo(
                packageName = packageName,
                name = name,
                fullName = fullName
            )
        )

        root += componentInfo

        classDeclaration.declarations.filterIsInstance<KSFunctionDeclaration>().forEach {
            val functionInfo = FunctionInfo()
            it.accept(FunctionVisitor(kspLogger), functionInfo)
            componentInfo.functionInfoList += functionInfo
        }

        val fileSpec = FileSpec.builder(packageName, componentName).apply {
            addType(
                TypeSpec.classBuilder(componentName).apply {
                    componentInfo.functionInfoList.forEach {
                        it.outParameterInfo?.let { (packageName, name) ->
                            addImport(packageName, name)
                        }
                    }
                    addSuperinterface(
                        componentInfo.parameterInfo.asClassName()
                    )
                    componentInfo.functionInfoList.forEach {
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
                                        append("return null!!")
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
}