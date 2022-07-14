package information

import com.squareup.kotlinpoet.ClassName

data class ClassInfo(
    //package is a reserved word
    var pack: String = "",
    var name: String = ""
) {
    val qualified: String
        get() = "$pack.$name"

    val className: ClassName
        get() = ClassName(pack, name)
}