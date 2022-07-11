package visitor

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.visitor.KSDefaultVisitor
import information.Dependency

class ConstructorVisitor(
    private val kspLogger: KSPLogger
) : KSDefaultVisitor<Dependency, Unit>() {
    override fun defaultHandler(node: KSNode, data: Dependency) {
        kspLogger.error("Should not happen")
    }

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Dependency) {
        val name = classDeclaration.simpleName.getShortName()
        val packageName = classDeclaration.packageName.asString()
        val fullName = "$packageName.$name"
        data.dependencies += Dependency(
            packageName = packageName,
            name = name,
            fullName = fullName
        )
    }

    override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: Dependency) {
        kspLogger.warn("Function: ${function.simpleName.getShortName()}")
        function.parameters.forEach {
            it.accept(this, data)
        }
    }

    override fun visitValueParameter(valueParameter: KSValueParameter, data: Dependency) {
        kspLogger.warn("Value: ${valueParameter.name?.getShortName()}")
        valueParameter.type.accept(this, data)
    }

    override fun visitTypeReference(typeReference: KSTypeReference, data: Dependency) {
        kspLogger.warn("Type: $typeReference")
        //resolve type to know it's class
        val resolvedType = typeReference.resolve().declaration
        kspLogger.warn("Resolved type: $resolvedType")
        if (resolvedType is KSClassDeclaration) {
            resolvedType.accept(this, data)
        }
    }
}