package information

data class Root(
    val factories: HashMap<String, FactoryInfo> = hashMapOf(),
    val components: HashMap<String, ComponentInfo> = hashMapOf()
) {
    operator fun plusAssign(factoryInfo: FactoryInfo) {
        factories[factoryInfo.parameterInfo.fullName] = factoryInfo
    }
    operator fun plusAssign(componentInfo: ComponentInfo) {
        components[componentInfo.parameterInfo.fullName] = componentInfo
    }
}