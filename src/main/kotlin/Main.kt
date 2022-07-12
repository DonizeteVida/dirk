import dummy.DirkMyComponent
import dummy.MyComponent

fun main(args: Array<String>) {
    val component: MyComponent = DirkMyComponent()
    val car = component.getCar()
    println("Hello World!")
    println("Program arguments: ${args.joinToString()}")
}