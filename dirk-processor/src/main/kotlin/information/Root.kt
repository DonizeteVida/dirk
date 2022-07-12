package information

data class Root(
    val factories: HashMap<String, FactoryInfo> = hashMapOf(),
    val components: HashMap<String, ComponentInfo> = hashMapOf()
) {
    operator fun plusAssign(factoryInfo: FactoryInfo) {
        factories[factoryInfo.classInfo.fullName] = factoryInfo
    }
    operator fun plusAssign(componentInfo: ComponentInfo) {
        components[componentInfo.classInfo.fullName] = componentInfo
    }
}