import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.asClassName

private val factory = Factory::class.asClassName()
fun ClassName.asFactoryParam() = factory.parameterizedBy(this)

fun KSClassDeclaration.getDeclaredClasses(): Sequence<KSClassDeclaration> =
    declarations.filterIsInstance<KSClassDeclaration>()