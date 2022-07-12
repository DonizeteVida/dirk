package visitor.component

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.writeTo
import visitor.FunctionVisitor
import information.ComponentInfo
import information.FunctionInfo
import information.ParameterInfo
import information.Root
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

        val componentName = "Dirk${componentInfo.parameterInfo.name}"

        root += componentInfo

        classDeclaration.declarations.filterIsInstance<KSFunctionDeclaration>().forEach {
            val functionInfo = FunctionInfo()
            it.accept(functionVisitor, functionInfo)
            componentInfo.functionInfoList += functionInfo
        }

        val fileSpec = FileSpec.builder(componentInfo.parameterInfo.packageName, componentName).apply {
            addType(
                TypeSpec.classBuilder(componentName).apply {
                    addSuperinterface(
                        componentInfo.parameterInfo.asClassName()
                    )
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