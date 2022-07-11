package information

data class Dependency(
    val packageName: String = "",
    val name: String = "",
    val dependencies: ArrayList<String> = arrayListOf()
)