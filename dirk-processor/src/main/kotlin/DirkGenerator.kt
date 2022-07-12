import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import visitor.component.ComponentGeneratorVisitor
import information.Root
import visitor.ClassVisitor
import visitor.FunctionVisitor
import visitor.inject.FactoryGeneratorVisitor

class DirkGenerator(
    private val environment: SymbolProcessorEnvironment
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val codeGenerator = environment.codeGenerator
        val logger = environment.logger

        val root = Root()

        val classVisitor = ClassVisitor(logger)
        val functionVisitor = FunctionVisitor(logger, classVisitor)

        val factoryGeneratorVisitor = FactoryGeneratorVisitor(
            codeGenerator,
            logger,
            root,
            classVisitor,
            functionVisitor
        )
        val componentGeneratorVisitor = ComponentGeneratorVisitor(
            codeGenerator,
            logger,
            root,
            classVisitor,
            functionVisitor
        )

        resolver.getSymbolsWithAnnotation(
            Inject::class.qualifiedName!!
        ).filterIsInstance<KSClassDeclaration>().forEach {
            it.accept(factoryGeneratorVisitor, Unit)
        }
        resolver.getSymbolsWithAnnotation(
            Component::class.qualifiedName!!
        ).filterIsInstance<KSClassDeclaration>().forEach {
            it.accept(componentGeneratorVisitor, Unit)
        }
        return emptyList()
    }
}