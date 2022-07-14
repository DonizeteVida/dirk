package visitor

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.visitor.KSDefaultVisitor
import com.squareup.kotlinpoet.ksp.toAnnotationSpec
import information.FunctionInfo
import information.ClassInfo

class FunctionVisitor (
    private val kspLogger: KSPLogger,
    private val classVisitor: ClassVisitor
) : KSDefaultVisitor<FunctionInfo, Unit>() {
    override fun defaultHandler(node: KSNode, data: FunctionInfo) {
        kspLogger.error("Function Visitor")
    }

    override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: FunctionInfo) {
        val name = function.simpleName.getShortName()
        data.name = name

        //in
        function.parameters.forEach {
            val resolvedParameter = it.type.resolve().declaration
            if (resolvedParameter is KSClassDeclaration) {
                val classInfo = ClassInfo()
                resolvedParameter.accept(classVisitor, classInfo)
                data += classInfo
            }
        }

        //out
        val returnDeclaration = function.returnType?.resolve()?.declaration
        if (returnDeclaration is KSClassDeclaration) {
            returnDeclaration.accept(classVisitor, data.outClassInfo)
        }

        //It'll be improved
        function.annotations.forEach {
            data.annotations += it.toAnnotationSpec()
        }
    }
}