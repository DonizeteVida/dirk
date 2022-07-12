package information

data class FactoryInfo(
    val classInfo: ClassInfo = ClassInfo(),
    val functionInfo: FunctionInfo = FunctionInfo()
) {
    override fun toString(): String {
        return "FactoryInfo(parameterInfo=$classInfo, functionInfo=$functionInfo)\n"
    }
}