package dummy

import Component
import dummy2.Bread

@Component
interface MyComponent {
    fun getCar(): Car
    fun getGas(): Gas
    fun getEngine(): Engine
    fun getMan(): Man
    fun getBread(): Bread
}