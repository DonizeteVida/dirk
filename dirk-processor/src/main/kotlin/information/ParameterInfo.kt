package information

import com.squareup.kotlinpoet.ClassName

data class ParameterInfo(
    var packageName: String = "",
    var name: String = "",
    var fullName: String = "",
) {
    fun asClassName() = ClassName(packageName, name)

    override fun toString(): String {
        return "ParameterInfo(packageName='$packageName', name='$name', fullName='$fullName')"
    }
}