package information

data class FunctionInfo(
    var name: String = "",
    val inClassInfoList: HashMap<String, ClassInfo> = hashMapOf(),
    val outClassInfo: ClassInfo = ClassInfo()
) {
    operator fun plusAssign(classInfo: ClassInfo) {
        inClassInfoList[classInfo.fullName] = classInfo
    }
}