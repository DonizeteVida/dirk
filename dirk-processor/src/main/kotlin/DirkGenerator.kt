import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import factory.component.ComponentGeneratorVisitor
import information.Root
import factory.inject.FactoryGeneratorVisitor

class DirkGenerator(
    private val environment: SymbolProcessorEnvironment
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val root = Root()
        resolver.getSymbolsWithAnnotation(
            Inject::class.qualifiedName!!
        ).filterIsInstance<KSClassDeclaration>().forEach {
            it.accept(FactoryGeneratorVisitor(environment.codeGenerator, environment.logger, root), Unit)
        }
        resolver.getSymbolsWithAnnotation(
            Component::class.qualifiedName!!
        ).filterIsInstance<KSClassDeclaration>().forEach {
            it.accept(ComponentGeneratorVisitor(environment.codeGenerator, environment.logger, root), Unit)
        }
        return emptyList()
    }
}