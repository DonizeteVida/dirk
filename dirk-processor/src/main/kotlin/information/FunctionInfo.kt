package information

data class FunctionInfo(
    var name: String = "",
    var inParameterInfoList: ArrayList<ParameterInfo> = arrayListOf(),
    var outParameterInfo: ParameterInfo? = null
)