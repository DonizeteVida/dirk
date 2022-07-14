package dummy2

import Module
import Provides
import dummy.Car

@Module
object MyModule {
    @Provides
    fun buildSomeThing(car: Car) = ByModule(car)
}