# Style

Follow the black style guide. Its rule index, and the Java specifics:

@~/github/black/INDEX.md
@~/github/java/black-java/guide/INDEX.md

Open a rule's file (paths are relative to ~/github/black, and to black-java's guide/) only when the rule is in play.

Layout is the black-java formatter's: run `./gradlew format`; `./gradlew check` fails on unformatted code and on
compiler warnings.

## This project

- No wildcard imports; import each type directly.
- Most interfaces here have a real implementation and a `Demo*` one, which the admin's demo mode wires in: those are
  seams. A new interface needs a reason like that (see design/seams), not "it might be expanded".
