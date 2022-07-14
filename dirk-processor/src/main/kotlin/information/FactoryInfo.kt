package information

data class FactoryInfo(
    val classInfo: ClassInfo = ClassInfo(),
    val functionInfo: FunctionInfo = FunctionInfo(),
    val additionalImports: ArrayList<ClassInfo> = arrayListOf(),
    val statementBuilder: FactoryInfo.() -> String,
)