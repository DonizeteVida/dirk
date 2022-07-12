package information

data class ComponentInfo(
    val classInfo: ClassInfo = ClassInfo(),
    val componentBuilderInfo: ComponentBuilderInfo = ComponentBuilderInfo(),
    val functionInfoList: ArrayList<FunctionInfo> = arrayListOf()
)