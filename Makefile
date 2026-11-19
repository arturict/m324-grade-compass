# Hier erstellt ihr alle Befehle damit die komplette Entwicklungsumgebung, installiert und gestartet werden kann
# Die einzelnen Schrite um die Anwedungen zu starten findet ihr im README.md
.PHONY: test clean

test: 
	cd backend && mvn test

clean:
	cd backend && mvn clean