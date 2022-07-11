package factory

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.visitor.KSDefaultVisitor
import information.ParameterInfo

class ClassVisitor(
    private val kspLogger: KSPLogger
) : KSDefaultVisitor<ParameterInfo, Unit>() {
    override fun defaultHandler(node: KSNode, data: ParameterInfo) {
        kspLogger.error("Class visitor")
    }

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: ParameterInfo) {
        val name = classDeclaration.simpleName.getShortName()
        val packageName = classDeclaration.packageName.asString()
        val fullName = "$packageName.$name"

        data.name = name
        data.packageName = packageName
        data.fullName = fullName
    }
}