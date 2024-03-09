package matt.async.co.scope

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

/*
- 100% Idiomatic
- Job() would be provided anyway, even if I used [[EmptyCoroutineContext]]
- Dispatchers.Default will be used ... by default
- Just make sure to cancel it when done (consider requiring a ShutdownContext and setting that up here instead.)
- CONSIDER RENAMING SHUTDOWN CONTEXT TO LIFECYCLE CONTEXT! Or maybe LifecycleContext can be a sub-interface of ShutdownContext with additional features!
*/
fun MyScope() = CoroutineScope(Job())

