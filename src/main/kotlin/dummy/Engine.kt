package dummy

import Inject

@Inject
class Engine {
    operator fun invoke() {
        println("Dummmm!!!")
    }
}