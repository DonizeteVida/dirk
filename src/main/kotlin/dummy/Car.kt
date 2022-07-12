package dummy

import javax.inject.Inject

class Car @Inject constructor(
    private val engine: Engine,
    private val gas: Gas
) {
    operator fun invoke() {
        println("Vrummm!!!")
        engine()
        gas()
    }
}