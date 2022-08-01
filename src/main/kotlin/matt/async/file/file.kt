package matt.async.file

import matt.async.date.Duration
import matt.async.every
import matt.file.MFile
import matt.file.recursiveLastModified


fun MFile.onModify(checkFreq: Duration, op: ()->Unit) {
  var lastModified = recursiveLastModified()
  every(checkFreq) {
	val mod = recursiveLastModified()
	if (mod != lastModified) {
	  op()
	}
	lastModified = mod
  }
}

