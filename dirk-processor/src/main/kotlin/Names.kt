import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.asClassName

object Names {
    private val FACTORY = Factory::class.asClassName()
    fun FACTORY_BY(className: ClassName) = FACTORY.parameterizedBy(className)
}