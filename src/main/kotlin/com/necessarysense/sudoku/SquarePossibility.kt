package com.necessarysense.sudoku

import kotlinx.collections.immutable.PersistentSet

sealed class SquarePossibility
data class Unresolved(val possibilities: PersistentSet<Char>) : SquarePossibility() {
    override fun toString(): String {
        return possibilities.joinToString("")
    }
}

data class Resolved(val actuality: Char) : SquarePossibility() {
    override fun toString(): String {
        return actuality.toString()
    }
}
