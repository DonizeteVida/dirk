package visitor.module

import Binds
import FactoryGenerator
import Provides
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.isConstructor
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import information.*
import visitor.ClassVisitor
import visitor.FunctionVisitor
import kotlin.reflect.KClass

class ModuleGeneratorVisitor(
    private val codeGenerator: CodeGenerator,
    private val kspLogger: KSPLogger,
    private val root: Root,
    private val classVisitor: ClassVisitor,
    private val functionVisitor: FunctionVisitor,
    private val factoryGenerator: FactoryGenerator
) : KSVisitorVoid() {

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
        if (false) {
            kspLogger.error(
                """
                @Module must be annotated only on companion classes
                or abstract classes
                Please verify: "${classDeclaration.simpleName.getShortName()}"
            """
            )
        }

        val classInfo = ClassInfo()

        classDeclaration.accept(classVisitor, classInfo)

        classDeclaration.getDeclaredFunctions().apply {
            filter(Provides::class).forEach {
                val functionInfo = FunctionInfo()
                it.accept(functionVisitor, functionInfo)

                val type = it.returnType?.resolve()?.declaration
                if (type is KSClassDeclaration) {
                    val factoryInfo = functionInfo.asFactoryInfo(
                        classInfo.name,
                        functionInfo.name,
                        classInfo
                    )

                    type.accept(factoryGenerator, factoryInfo)
                }
            }
            filter(Binds::class).forEach {
                TODO("Someday someday")
            }
        }
    }

    @OptIn(KspExperimental::class)
    fun <T : Annotation> Sequence<KSFunctionDeclaration>.filter(clazz: KClass<T>) = filter {
        it.isAnnotationPresent(clazz)
    }

    private fun FunctionInfo.asFactoryInfo(module: String, fn: String, classInfo: ClassInfo) =
        FactoryInfo(
            classInfo = outClassInfo,
            functionInfo = this,
            additionalImports = arrayListOf(classInfo)
        ) {
            "$module.$fn"
        }
}