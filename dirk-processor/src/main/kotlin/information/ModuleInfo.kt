package information

data class ModuleInfo(
    val classInfo: ClassInfo = ClassInfo(),
    val functionInfoList: HashMap<String, FunctionInfo> = hashMapOf()
) {
    operator fun plusAssign(functionInfo: FunctionInfo) {
        functionInfoList[functionInfo.name] = functionInfo
    }
}