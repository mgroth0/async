package matt.async.executor

import matt.lang.function.Op


interface NamingExecutor {
    fun namedExecution(
        name: String,
        op: Op
    )
}