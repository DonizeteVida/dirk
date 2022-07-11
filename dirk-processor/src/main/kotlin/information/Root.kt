package information

data class Root(
    val dependencies: HashMap<String, Dependency> = hashMapOf()
) {
    operator fun contains(name: String) = name in dependencies
    operator fun plusAssign(dependency: Dependency) {
        dependencies["${dependency.packageName}.${dependency.name}"] = dependency
    }
}