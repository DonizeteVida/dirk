package information

data class ComponentInfo(
    val parameterInfo: ParameterInfo = ParameterInfo(),
    val functionInfoList: ArrayList<FunctionInfo> = arrayListOf()
) {
    override fun toString(): String {
        return "ComponentInfo(\nparameterInfo=$parameterInfo,\nfunctionInfoList=${functionInfoList.joinToString("\n")}\n)"
    }
}