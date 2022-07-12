package dummy

import javax.inject.Inject

data class Man @Inject constructor(
    val car: Car,
    val empty: Empty
)