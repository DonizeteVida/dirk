package information

data class ComponentInfo(
    val classInfo: ClassInfo = ClassInfo(),
    val functionInfoList: ArrayList<FunctionInfo> = arrayListOf()
)