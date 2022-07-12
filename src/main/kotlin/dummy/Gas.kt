package dummy

import javax.inject.Inject

class Gas @Inject constructor(){
    operator fun invoke() {
        println("Glub glub!!!")
    }
}