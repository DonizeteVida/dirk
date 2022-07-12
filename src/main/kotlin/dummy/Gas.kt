package dummy

import Inject

@Inject
class Gas {
    operator fun invoke() {
        println("Glub glub!!!")
    }
}