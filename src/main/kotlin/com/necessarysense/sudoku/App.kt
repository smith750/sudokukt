package com.necessarysense.sudoku

fun main() {
    fun printDividers() {
        println("\n~~~~~~~\n")
    }

    val puzzle1 = Game.solve("..3.2.6..9..3.5..1..18.64....81.29..7.......8..67.82....26.95..8..2.3..9..5.1.3..")
    println(puzzle1.display())
    printDividers()
    val puzzle2 = Game.solve("4.....8.5.3..........7......2.....6.....8.4......1.......6.3.7.5..2.....1.4......")
    println(puzzle2.display())
    printDividers()
    val puzzle3 = Game.solve("..59.3..1.8..7..24..125.8.932...8.4..7.....5..5.3...965.2.417..41..3..6.6..8.24..")
    println(puzzle3.display())
    printDividers()
    val puzzle4 = Game.solve("....7..89..6.3..1...51..4........876..9...5..687........4..91...9..8.6..13..4....")
    println(puzzle4.display())
    printDividers()
    val puzzle5 = Game.solve("..5........3.69..5...3...8947...62......9......68...1782...4...1..93.8........4..")
    println(puzzle5.display())

    val genPuzzle = Board.generateRandom()
    println(genPuzzle.display())
}
