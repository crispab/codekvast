# Dynamic Useless Code Killer Spike

Visar att man kan använda AspectJ och lite reflectionmagi för att detektera död kod.

Spiken består av följande moduler:

* product/agent/sensor (en javaagent som innehåller aspectjweaver.jar)
* product/agent/duck-agent (en fristående java app som skickar insamlade användningsdata till datalagret)
* product/server/duck-server
* sample/standalone-app (en stand-alone Java-app med lite useless code)
* sample/tapestry5-war
* sample/vanilla-jsp (en WAR med ett par vanliga JSP-sidor)

## Hur man bygger

    ./gradlew build

## Hur man testar

I ett fönster startar man duck-server som tar emot data from duck-agent:

    ./gradlew :product:server:duck-server:run
    
I nästa fönster startar man duck-agent som laddar upp sensordata till duck-server:

    ./gradlew :product:agent:duck-agent:run

I det tredje fönstret kör man sample-app som startar med -javaagent:duck-sensor och som därmed genererar sensordata:

    ./gradlew :sample:standalone-app:run

Om allt fungerar skall det komma ut en sammanställning av useless code på slutet.

## Förutsättningar

Spiken kräver att minst Java 6 är installerat i PATH.
Utvecklat och testat med Oracle Java 7.

## Utvecklingsmiljö

* Java 7
* Lombok-plugin i IDE

# Lärdomar från spiken

Jag har haft möjlighet att testa DUCK-spiken på Transmode Network Manager, och funnit följande saker:

* Klasser som skickas över RMI (och som därför måste vara Serializable) **måste** ha serialVersionUID, annars krashar RMI-anropet på
klientsidan (om inte den också kör DUCK förstås).

* Guice AOP är ganska aggressivt i sin bytekodmanipulering. Den genererar i runtime nya subklasser med metoder som inte finns med i källkoden.
Detta måste DUCK hantera.

* Frågan är hur det är med andra ramverk som gör bytekodmanipulering: Spring, JBoss, Tapestry5 etc. Vi behöver bygga en realistisk sample app.



