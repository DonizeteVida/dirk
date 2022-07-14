package information

import com.squareup.kotlinpoet.AnnotationSpec

data class FunctionInfo(
    var name: String = "",
    val inClassInfoList: HashMap<String, ClassInfo> = hashMapOf(),
    val outClassInfo: ClassInfo = ClassInfo(),
    val annotations: HashSet<AnnotationSpec> = hashSetOf()
) {
    operator fun plusAssign(classInfo: ClassInfo) {
        inClassInfoList[classInfo.qualified] = classInfo
    }
}