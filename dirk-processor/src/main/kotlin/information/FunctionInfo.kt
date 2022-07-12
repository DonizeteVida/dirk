package information

data class FunctionInfo(
    var name: String = "",
    var inParameterInfoList: HashMap<String, ParameterInfo> = hashMapOf(),
    var outParameterInfo: ParameterInfo? = null
) {
    operator fun plusAssign(parameterInfo: ParameterInfo) {
        inParameterInfoList[parameterInfo.fullName] = parameterInfo
    }
}