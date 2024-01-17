package matt.async.pri

var Thread.myPrioririty: MyThreadPriorities
    get() = MyThreadPriorities.entries[priority]
    set(value) {
        priority = value.ordinal
    }
