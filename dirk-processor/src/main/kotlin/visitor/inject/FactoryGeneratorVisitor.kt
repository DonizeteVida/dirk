package visitor.inject

import Names.FACTORY_BY
import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.isConstructor
import com.google.devtools.ksp.isInternal
import com.google.devtools.ksp.isLocal
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.writeTo
import information.FactoryInfo
import information.Root
import visitor.ClassVisitor
import visitor.FunctionVisitor

class FactoryGeneratorVisitor(
    private val codeGenerator: CodeGenerator,
    private val kspLogger: KSPLogger,
    private val root: Root,
    private val classVisitor: ClassVisitor,
    private val functionVisitor: FunctionVisitor
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

        //here will be where all fuckness will happen
        //we are visiting the class which was annotated with @Inject
        //what should we do?
        //we only need to get knowledge about its constructor
        //and about the class itself
        //so first we will visit this class to know itself
        //later it we will visit its constructor
        //it will allow us to know how to build it

        val factoryInfo = FactoryInfo()

        //here we visit the class itself
        //to know things like name, package name, etc
        //you may say: but doni, my friend, you are already visiting
        //the class, why visit it again?
        //because the code is break into little pieces
        //to be reused more friendly
        //each visitor is capable to do one thing, only
        //I like this way
        classDeclaration.accept(classVisitor, factoryInfo.classInfo)
        classDeclaration.primaryConstructor?.accept(functionVisitor, factoryInfo.functionInfo)

        root += factoryInfo

        val factoryName = "${factoryInfo.classInfo.name}_Factory"

        val fileSpec = FileSpec
            .builder(factoryInfo.classInfo.packageName, factoryName)
            .apply {
                addImport(packageName, name)
                factoryInfo.functionInfo.inClassInfoList.values.forEach { (packageName, name) ->
                    addImport(packageName, name)
                }

                addType(
                    TypeSpec.classBuilder(factoryName)
                        .apply {
                            primaryConstructor(
                                FunSpec.constructorBuilder().apply {
                                    factoryInfo.functionInfo.inClassInfoList.values.forEach {
                                        val lowercase = it.name.lowercase()
                                        val parameter = FACTORY_BY(it.asClassName())
                                        addParameter(
                                            lowercase,
                                            parameter,
                                        )
                                        addProperty(
                                            PropertySpec.builder(
                                                lowercase,
                                                parameter,
                                                KModifier.PRIVATE
                                            ).initializer(
                                                lowercase
                                            ).build()
                                        )
                                    }
                                }.build()
                            )
                            addSuperinterface(
                                FACTORY_BY(factoryInfo.classInfo.asClassName())
                            )
                            addFunction(
                                FunSpec.builder("invoke").apply {
                                    addModifiers(
                                        KModifier.OVERRIDE,
                                        KModifier.OPERATOR
                                    )
                                    addStatement(
                                        StringBuilder().apply {
                                            append("return ${factoryInfo.classInfo.name}(")
                                            val str = factoryInfo
                                                .functionInfo
                                                .inClassInfoList
                                                .values
                                                .joinToString(", ") {
                                                    "${it.name.lowercase()}()"
                                                }
                                            append(str)
                                            append(")")
                                        }.toString()
                                    )
                                }.build()
                            )
                        }.build()
                )
            }.build()

        fileSpec.writeTo(
            codeGenerator,
            Dependencies(true, classDeclaration.containingFile!!)
        )
    }
}