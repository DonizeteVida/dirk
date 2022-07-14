import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import information.Root
import visitor.ClassVisitor
import visitor.FunctionVisitor
import visitor.component.ComponentGeneratorVisitor
import visitor.inject.InjectGeneratorVisitor
import visitor.module.ModuleGeneratorVisitor
import javax.inject.Inject

class DirkGenerator(
    private val environment: SymbolProcessorEnvironment
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val codeGenerator = environment.codeGenerator
        val logger = environment.logger

        val root = Root()

        val classVisitor = ClassVisitor(logger)
        val functionVisitor = FunctionVisitor(logger, classVisitor)

        val factoryGenerator = FactoryGenerator(
            codeGenerator,
            logger,
            root,
            classVisitor
        )
        val injectGeneratorVisitor = InjectGeneratorVisitor(
            codeGenerator,
            logger,
            root,
            functionVisitor,
            factoryGenerator
        )
        val moduleGeneratorVisitor = ModuleGeneratorVisitor(
            codeGenerator,
            logger,
            root,
            classVisitor,
            functionVisitor,
            factoryGenerator
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
        ).filterIsInstance<KSFunctionDeclaration>().forEach {
            it.accept(injectGeneratorVisitor, Unit)
        }
        resolver.getSymbolsWithAnnotation(
            Module::class.qualifiedName!!
        ).filterIsInstance<KSClassDeclaration>().forEach {
            it.accept(moduleGeneratorVisitor, Unit)
        }
        resolver.getSymbolsWithAnnotation(
            Component::class.qualifiedName!!
        ).filterIsInstance<KSClassDeclaration>().forEach {
            it.accept(componentGeneratorVisitor, Unit)
        }
        return emptyList()
    }
}