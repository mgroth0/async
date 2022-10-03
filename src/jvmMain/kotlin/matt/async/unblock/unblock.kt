package matt.async.unblock

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStream

suspend fun InputStream.readSuspending() = withContext(Dispatchers.IO) { this@readSuspending.read().takeIf { it >= 0 }?.toChar() }
suspend fun BufferedReader.readSuspending() = withContext(Dispatchers.IO) { this@readSuspending.read().takeIf { it >= 0 }?.toChar() }

suspend fun BufferedReader.readLineSuspending(): String? = withContext(Dispatchers.IO) { readLine() }