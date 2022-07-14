package visitor.inject

import FactoryGenerator
import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.isConstructor
import com.google.devtools.ksp.isInternal
import com.google.devtools.ksp.isLocal
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import information.FactoryInfo
import information.Root
import visitor.FunctionVisitor

class InjectGeneratorVisitor(
    private val codeGenerator: CodeGenerator,
    private val kspLogger: KSPLogger,
    private val root: Root,
    private val functionVisitor: FunctionVisitor,
    private val factoryGenerator: FactoryGenerator
) : KSVisitorVoid() {
    override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: Unit) {
        //@Inject on a constructor will trigger
        //as it is a function (because a constructor is a function)
        //so we will get its return type (which is itself) and visit it's class
        //which is itself again
        //it's because javax's @Inject annotation is only allowed on a constructor
        //big day
        if (!function.isConstructor()) {
            kspLogger.error("@Inject must only be annotated on a constructor: ${function.simpleName.getShortName()}")
        }

        //resolve() give us the original class
        //as we need to visit it and get information
        val type = function.returnType?.resolve()?.declaration
        if (type is KSClassDeclaration) {
            type.accept(this, Unit)
        }
    }

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
        //I don't know what more is invalid
        //It's okay for a while
        if (classDeclaration.run {
            isAbstract() or
            isLocal() or
            isCompanionObject or
            isInternal()
        }) {
            kspLogger.error("@Inject must be annotated on a valid class: ${classDeclaration.simpleName.getShortName()}")
        }
        val factoryInfo = FactoryInfo {
            classInfo.name
        }

        classDeclaration.primaryConstructor?.accept(functionVisitor, factoryInfo.functionInfo)

        classDeclaration.accept(factoryGenerator, factoryInfo)
    }
}