package visitor.module

import FactoryGenerator
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.isConstructor
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import information.FactoryInfo
import information.FunctionInfo
import information.ModuleInfo
import information.Root
import visitor.ClassVisitor
import visitor.FunctionVisitor

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
            kspLogger.error("""
                @Module must be annotated only on companion classes
                or abstract classes
                Please verify: "${classDeclaration.simpleName.getShortName()}"
            """)
        }

        val moduleInfo = ModuleInfo()

        classDeclaration.accept(classVisitor, moduleInfo.classInfo)

        root += moduleInfo

        classDeclaration.getDeclaredFunctions().forEach {
            if (it.isConstructor()) return@forEach

            val functionInfo = FunctionInfo()
            it.accept(functionVisitor, functionInfo)
            moduleInfo += functionInfo

            val type = it.returnType?.resolve()?.declaration
            if (type is KSClassDeclaration) {
                val factoryInfo = functionInfo.asFactoryInfo(
                    moduleInfo.classInfo.name,
                    functionInfo.name
                )

                factoryInfo.additionalImports += moduleInfo.classInfo

                type.accept(factoryGenerator, factoryInfo)
            }
        }
    }

    private fun FunctionInfo.asFactoryInfo(module: String, fn: String) =
        FactoryInfo(
            classInfo = outClassInfo,
            functionInfo = this
        ) {
            "$module.$fn"
        }
}