package matt.async.flow

import kotlinx.coroutines.flow.Flow

/*Matt's suspend versions of kotlin.sequences._Sequences*/

suspend fun <T> Flow<T>.joinToString(
  separator: CharSequence = ", ",
  prefix: CharSequence = "",
  postfix: CharSequence = "",
  limit: Int = -1,
  truncated: CharSequence = "...",
  transform: ((T)->CharSequence)? = null
): String {
  return joinTo(StringBuilder(), separator, prefix, postfix, limit, truncated, transform).toString()
}

@Suppress("BlockingMethodInNonBlockingContext")
suspend fun <T, A: Appendable> Flow<T>.joinTo(
  buffer: A,
  separator: CharSequence = ", ",
  prefix: CharSequence = "",
  postfix: CharSequence = "",
  limit: Int = -1,
  truncated: CharSequence = "...",
  transform: ((T)->CharSequence)? = null
): A {
//  println("joinTo debug 1: $buffer")
  buffer.append(prefix)
//  println("joinTo debug 2: $buffer")
  var count = 0
//  println("joinTo debug 3: $buffer")
  collect { element ->
//	println("joinTo debug 4: $buffer")
	if (++count > 1) buffer.append(separator)
//	println("joinTo debug 5: $buffer")
	if (limit < 0 || count <= limit) {
//	  println("joinTo debug 6: $buffer")
	  buffer.appendElement(element, transform)
//	  println("joinTo debug 7: $buffer")
	} else return@collect
//	println("joinTo debug 8: $buffer")
  }
//  println("joinTo debug 9: $buffer")
  if (limit in 0 until count) buffer.append(truncated)
//  println("joinTo debug 10: $buffer")
  buffer.append(postfix)
//  println("joinTo debug 11: $buffer")
  return buffer
}

fun <T> Appendable.appendElement(element: T, transform: ((T)->CharSequence)?) {
  when {
	transform != null        -> append(transform(element))
	element is CharSequence? -> append(element)
	element is Char          -> append(element)
	else                     -> append(element.toString())
  }
}
