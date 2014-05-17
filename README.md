# Dynamic Useless Code Killer Spike

Visar att man kan använda AspectJ och lite reflectionmagi för att detektera död kod.

Spiken består av tre moduler:

* duck-sensor (en javaagent som innehåller aspectjweaver.jar)
* duck-agent (en fristående java app som skickar insamlade användningsdata till datalagret)
* sample-app (en stand-alone Java-app med lite useless code)

## Hur man bygger

./gradlew build

## Hur man testar

./gradlew run

Om allt fungerar skall det komma ut en sammanställning av useless code på slutet.

## Förutsättningar

Spiken kräver att minst Java 6 är installerat i PATH.
Utvecklat och testat med Oracle Java 7.

## Utvecklingsmiljö

* Java 6
* Lombok-plugin i IDE

