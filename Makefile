# Hier erstellt ihr alle Befehle damit die komplette Entwicklungsumgebung, installiert und gestartet werden kann
# Die einzelnen Schrite um die Anwedungen zu starten findet ihr im README.md

# Lokale Einstellungen wie SONAR_TOKEN stehen in .env (nicht im Git, siehe .gitignore)
-include .env

# SonarScanner läuft in einem Container im Netzwerk sonar-net. Dort heisst der
# SonarQube-Server "sonarqube" (Container-Name); "localhost" wäre der Scanner-Container selbst.
SONAR_HOST_URL ?= http://sonarqube:9000
SONAR_NETWORK ?= sonar-net
SONAR_TOKEN ?=

# Docker Image des Backends
IMAGE ?= grade-compass-backend
TAG ?= latest
CONTAINERS ?= notenkompass-backend notenkompass-backend-2 notenkompass-frontend

# Docker Image des Frontends (Angular, ausgeliefert mit nginx)
FRONTEND_IMAGE ?= grade-compass-frontend

.PHONY: test clean coverage linting docker docker-frontend docker-clean

test:
	cd backend && mvn test

clean:
	cd backend && mvn clean

coverage:
	mvn -f backend/pom.xml test
	mvn -f backend/pom.xml dependency:copy-dependencies -DoutputDirectory=target/dependency

linting: coverage
	docker run --rm \
		-e SONAR_HOST_URL="$(SONAR_HOST_URL)" \
		-e SONAR_TOKEN="$(SONAR_TOKEN)" \
		--network $(SONAR_NETWORK) \
		-v .:/usr/src \
		-u $$(id -u):$$(id -g) \
		sonarsource/sonar-scanner-cli

docker:
	docker build -t $(IMAGE):$(TAG) backend

docker-frontend:
	docker build -t $(FRONTEND_IMAGE):$(TAG) frontend

# Entfernt die Container, die Datenbank (Daten bleiben im Volume) und alle Tags der Images.
# "docker rmi" bricht bei fehlenden Images ab, deshalb nur vorhandene IDs via xargs -r löschen.
docker-clean:
	docker rm -f $(CONTAINERS)
	docker compose -f backend/compose.yaml down
	docker images -q $(IMAGE) | sort -u | xargs -r docker rmi -f
	docker images -q $(FRONTEND_IMAGE) | sort -u | xargs -r docker rmi -f
