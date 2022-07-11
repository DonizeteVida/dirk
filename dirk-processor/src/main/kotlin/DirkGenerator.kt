import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import information.Root
import visitor.ClassVisitor

class DirkGenerator(
    private val codeGenerator: CodeGenerator,
    private val kspLogger: KSPLogger
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val root = Root()
        val symbol = resolver.getSymbolsWithAnnotation(
            Inject::class.qualifiedName!!
        )
        symbol.filterIsInstance<KSClassDeclaration>().forEach {
            it.accept(ClassVisitor(codeGenerator, kspLogger, root), Unit)
        }

        return emptyList()
    }
}