package information

import com.squareup.kotlinpoet.ClassName

data class Dependency(
    val packageName: String = "",
    val name: String = "",
    val fullName: String = "",
    val dependencies: ArrayList<Dependency> = arrayListOf()
) {
    override fun toString(): String {
        return "Dependency(\n" +
                "packageName='$packageName',\n" +
                "name='$name',\n" +
                "fullName='$fullName',\n" +
                "dependencies=$dependencies\n" +
                ")"
    }
}

fun Dependency.asClassName() = ClassName(packageName, name)