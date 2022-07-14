import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.visitor.KSDefaultVisitor
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.writeTo
import information.FactoryInfo
import information.Root
import visitor.ClassVisitor


/**
 * Both @Module and @Inject are capable to
 * generate "Factory" classes.
 * The idea here is to concentrate Factory<T>
 * logic's on one place, only.
 * We don't care honestly for who is creating
 * */
class FactoryGenerator(
    private val codeGenerator: CodeGenerator,
    private val kspLogger: KSPLogger,
    private val root: Root,
    private val classVisitor: ClassVisitor
) : KSDefaultVisitor<FactoryInfo, Unit>() {
    override fun defaultHandler(node: KSNode, data: FactoryInfo) {
        kspLogger.error("FactoryGenerator")
    }

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: FactoryInfo) {
        root += data

        val factoryName = "${data.classInfo.name}_Factory"

        FileSpec
            .builder(data.classInfo.pack, factoryName)
            .apply {
                addImport(packageName, name)
                data.functionInfo.inClassInfoList.values.forEach { (packageName, name) ->
                    addImport(packageName, name)
                }
                data.additionalImports.forEach {
                    addImport(it.pack, it.name)
                }
                addType(
                    TypeSpec.classBuilder(factoryName)
                        .apply {
                            primaryConstructor(
                                FunSpec.constructorBuilder().apply {
                                    data.functionInfo.inClassInfoList.values.forEach {
                                        val lowercase = it.name.lowercase()
                                        val parameter = it.className.asFactoryParam()
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
                                data.classInfo.className.asFactoryParam()
                            )
                            addFunction(
                                FunSpec.builder("invoke").apply {
                                    addModifiers(
                                        KModifier.OVERRIDE,
                                        KModifier.OPERATOR
                                    )
                                    val str = StringBuilder().apply {
                                        append("return ${data.statementBuilder(data)}(")
                                        val str = data
                                            .functionInfo
                                            .inClassInfoList
                                            .values
                                            .joinToString(", ") {
                                                "${it.name.lowercase()}()"
                                            }
                                        append(str)
                                        append(")")
                                    }.toString()
                                    addStatement(str)
                                }.build()
                            )
                        }.build()
                )
            }
            .build()
            .writeTo(
                codeGenerator,
                Dependencies(
                    true, classDeclaration.containingFile!!
                )
            )
    }
}
