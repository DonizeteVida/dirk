package visitor

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.visitor.KSDefaultVisitor
import information.ClassInfo

class ClassVisitor(
    private val kspLogger: KSPLogger
) : KSDefaultVisitor<ClassInfo, Unit>() {
    override fun defaultHandler(node: KSNode, data: ClassInfo) {
        kspLogger.error("Class visitor")
    }

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: ClassInfo) {
        val name = classDeclaration.simpleName.getShortName()
        val pack = classDeclaration.packageName.getShortName()

        data.name = name
        data.pack = pack
    }
}