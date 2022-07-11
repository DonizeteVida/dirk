package dummy

import com.inject.dirk.annotation.Inject

@Inject
class Car (
    private val engine: Engine,
    private val gas: Gas
)