package matt.async.co.app

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import matt.async.co.scope.MyScope
import matt.lang.debug.js.properlyLogErrorStackTracesSuspending

/*

* OFFICIAL JS code for creating Coroutine Scope: https://github.com/Kotlin/kotlinx.coroutines/blob/master/js/example-frontend-js/src/ExampleMain.kt

*/
val Application by lazy {
    val scope = MyScope()
    scope.launch {
        properlyLogErrorStackTracesSuspending {
            awaitCancellation()
        }
    }
    scope
}
