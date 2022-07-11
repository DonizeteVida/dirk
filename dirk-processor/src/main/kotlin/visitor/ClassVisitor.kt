package visitor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.*
import information.Dependency
import information.Root

class ClassVisitor(
    private val codeGenerator: CodeGenerator,
    private val kspLogger: KSPLogger,
    private val root: Root
) : KSVisitorVoid() {
    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
        val packageName = classDeclaration.packageName.asString()
        val name = classDeclaration.simpleName.getShortName()

        kspLogger.warn("Class: ${packageName}/${name}")

        val file = codeGenerator.createNewFile(
            Dependencies(true, classDeclaration.containingFile!!),
            packageName,
            "${name}_Factory"
        )

        val dependency = Dependency(
            packageName = packageName,
            name = name
        )
        classDeclaration.primaryConstructor?.accept(this, Unit)
        file.write("//$dependency".toByteArray())
        file.close()
    }

    override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: Unit) {
        kspLogger.warn("Function: ${function.simpleName.getShortName()}")
        function.parameters.forEach {
            it.accept(this, Unit)
        }
    }

    override fun visitValueParameter(valueParameter: KSValueParameter, data: Unit) {
        kspLogger.warn("Value: ${valueParameter.name?.getShortName()}")
        //kspLogger.warn("Location: ${valueParameter.location}")
        valueParameter.type.accept(this, Unit)
    }

    override fun visitTypeReference(typeReference: KSTypeReference, data: Unit) {
        kspLogger.warn("Type: $typeReference")
        //resolve type to know it's class
        val resolvedType = typeReference.resolve().declaration
        kspLogger.warn("Resolved type: $resolvedType")
        if (resolvedType is KSClassDeclaration) {
            //resolvedType.accept(this, Unit)
        }
    }
}