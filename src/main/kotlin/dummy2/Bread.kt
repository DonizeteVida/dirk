package dummy2

import dummy.Car
import javax.inject.Inject

class Bread @Inject constructor(
    val car: Car
)