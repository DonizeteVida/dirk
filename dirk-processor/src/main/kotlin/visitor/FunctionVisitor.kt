package visitor

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.visitor.KSDefaultVisitor
import information.FunctionInfo
import information.ParameterInfo

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
                val parameterInfo = ParameterInfo()
                resolvedParameter.accept(classVisitor, parameterInfo)
                data += parameterInfo
            }
        }

        //out
        val returnDeclaration = function.returnType?.resolve()?.declaration
        if (returnDeclaration is KSClassDeclaration) {
            val parameterInfo = ParameterInfo()
            returnDeclaration.accept(classVisitor, parameterInfo)
            data.outParameterInfo = parameterInfo
        }
    }
}