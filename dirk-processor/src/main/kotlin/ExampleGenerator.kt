import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import java.io.OutputStream

fun OutputStream.append(str: String) = write(str.toByteArray())

class ExampleProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        ExampleGenerator(environment.codeGenerator, environment.logger)
}

class ExampleGenerator(
    private val codeGenerator: CodeGenerator,
    private val kspLogger: KSPLogger
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbol = resolver.getSymbolsWithAnnotation(
            "com.inject.dirk.annotation.Todo"
        )
        symbol.forEach {
            it.accept(CommentVisitor(), Unit)
        }
        return emptyList()
    }

    private inner class CommentVisitor : KSVisitorVoid() {
        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
            classDeclaration.primaryConstructor!!.parameters
            val name = classDeclaration.simpleName.getShortName()
            val file = codeGenerator.createNewFile(
                Dependencies(false, classDeclaration.containingFile!!),
                classDeclaration.containingFile!!.packageName.getShortName(),
                "${name}Generated"
            )
            file.append("//$name")
            file.close()
        }

    }
}