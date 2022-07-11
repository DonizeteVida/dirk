package dummy

import Inject

@Inject
class Car (
    private val engine: Engine,
    private val gas: Gas
)