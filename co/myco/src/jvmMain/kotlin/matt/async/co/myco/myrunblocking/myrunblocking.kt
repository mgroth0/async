@file:SeeURL(
    "https://youtrack.jetbrains.com/issue/KTIJ-7662/IDE-support-internal-visibility-introduced-by-associated-compilations"
)
@file:Suppress("invisible_reference", "invisible_member")

package matt.async.co.myco.myrunblocking

import kotlinx.coroutines.ThreadLocalEventLoop
import matt.lang.anno.SeeURL


fun justProvingIHaveFriendPathNow() {
    println("$ThreadLocalEventLoop is internal to kotlinx.coroutines, but it is my friend!!!")
}
