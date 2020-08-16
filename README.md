# Sudoku Solver

Based, of course, off of Peter Norvig's famous Sudoku solver: https://norvig.com/sudoku.html

## Lessons Learned

### Sealed Classes

I looked forward to using sealed classes in order to clarify the state of where we were in solving the board.  This obviously led to a number of places where I had to think about how to translate from the Python...but in retrospect, I feel like those places forced me to dig into the algorithm and understand it better.  I'm grateful for that.

But where I still feel like I fell down was differentiating how to access the sealed class values.  Kotlin points you straight at `when` - which is fine - but `when`'s all over the code are an obvious code smell, one I haven't really fixed yet, especially with `SquarePossibility`.

I had two ways of avoiding `when`: polymorphism and `Game.mapBoardPossibility`.  `Game.mapBoardPossibility` basically treats `BoardPossibility` as a variation of `Option`...which, of course, it is.  I liked the level of abstraction here, but I definitely need to improve my abilities on this count.  I could also define a method in `BoardPossibility` and implement that in both child classes.  This was useful when I wanted `ImpossibleBoard` to act differently than `Board` - really: `display`.  They're both useful but I need to keep on thinking about which technique is useful in which context.

### Immutable and Persistent Collections

The main lesson here is: I still have a great deal to learn about Immutable and Persistent Collections in Kotlin.  I had to do casts to make the plus and minus operators work and I was uncertain where I could refer to, say, "List" as read only and "PersistentList" as the specific implementation - or even what the difference between the Persistent and Immutable collections were.  Which is to say: I need to experiment a whole lot more. 

### Null Constraints

Protections around `null` are a big sellilng point of Kotlin.  But there were places where it was annoying and where I wish I could offer the compiler more guarantees.  For instance: every SquarePosition in SQUARES will return a value from the board; that's the way the board is set up.  And yet...I had to avoid null values, and I often used `!!` for convenience.  I wish I knew better how to handle this.
