package dummy

import javax.inject.Inject

class Engine @Inject constructor(){
    operator fun invoke() {
        println("Dummmm!!!")
    }
}