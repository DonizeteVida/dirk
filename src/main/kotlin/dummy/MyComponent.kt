package dummy

import BindsInstance
import Component
import dummy2.Bread
import dummy2.ByModule
import dummy2.Thing

@Component
interface MyComponent {
    interface Builder {
        @BindsInstance
        fun bindsThing(thing: Thing): Builder
        fun build(): MyComponent
    }

    fun getCar(): Car
    fun getGas(): Gas
    fun getEngine(): Engine
    fun getMan(): Man
    fun getBread(): Bread
    fun getByModule(): ByModule
}