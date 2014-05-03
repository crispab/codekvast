# Dynamic Useless Code Killer Spike

Visar att man kan använda AspectJ och lite reflectionmagi för att detektera död kod.

Spiken består av två moduler:

* duck-agent (en javaagent som innehåller aspectjweaver.jar)
* sample-app (en stand-alone Java-app med lite useless code)

## Hur man bygger

./gradlew build

## Hur man testar

./gradlew :sample-app:run

Om allt fungerar skall det komma ut en sammanställning av useless code på slutet.

## Förutsättningar

Spiken kräver att minst Java 5 är installerat i PATH. Utvecklat och testat med Oracle Java 7.

