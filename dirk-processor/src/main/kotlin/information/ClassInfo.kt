package information

import com.squareup.kotlinpoet.ClassName

data class ClassInfo(
    var packageName: String = "",
    var name: String = "",
    var fullName: String = "",
) {
    fun asClassName() = ClassName(packageName, name)
}