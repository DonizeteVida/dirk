package information

data class FactoryInfo(
    val parameterInfo: ParameterInfo = ParameterInfo(),
    val functionInfo: FunctionInfo = FunctionInfo()
) {
    override fun toString(): String {
        return "FactoryInfo(\nparameterInfo=$parameterInfo\n, parameterInfoList=\n$functionInfo})"
    }
}