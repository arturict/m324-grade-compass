# 4 Abhängigkeiten – Prüfungstraining

Branch `uebung-artur` · Ordner `/home/dev/school/m324/m324-grade-compass-uebung`

Hier machst du die **prüfungsrelevanten** Teile von Aufgabe 4 selbst, ohne die Basics zu wiederholen. Der Code steht auf dem Stand **vor** Aufgabe 4. Die vollständigen Lösungen mit allen Messungen stehen in [4.md](./4.md); die Lösungsfiles kannst du jederzeit mit `git show main:<pfad>` anschauen.

---

## So funktioniert das Training

### Startzustand

| Was | Hier (Training) | Auf `main` (Lösung) |
|---|---|---|
| `backend/Dockerfile`, `backend/.dockerignore` | fehlen | vorhanden |
| `Makefile` | nur `test`, `clean`, `coverage`, `linting` | zusätzlich `docker`, `docker-frontend`, `docker-clean` |
| `backend/pom.xml` | ohne Actuator | mit `spring-boot-starter-actuator` |
| `CreateSubjectRequest` | `@Size(max = 100)` | `@Size(max = 50)` |
| `frontend/Dockerfile`, `nginx.conf`, `.dockerignore` | fehlen | vorhanden |

### Eigene Namen und Ports (damit nichts mit `main` kollidiert)

| Ressource | Name im Training |
|---|---|
| Image | `uebung-backend` (Tags `latest`, `build`, `1.0`, `2.0`) |
| Datenbank-Container | `uebung-postgres` (kein Port nach aussen) |
| Backend-Container | `uebung-backend` → Port **18080**, `uebung-backend-2` → Port **18081** |
| Frontend-Container | `uebung-frontend` → Port **14200** |
| Netzwerk | `uebung-net` |
| DB-Passwort | `uebung` (DB `gcdb`, User `gcdbUser`) |

`backend/compose.yaml` wird im Training **nicht** verwendet: Dort sind `container_name` und Port `5432` fix, das würde mit der Datenbank auf `main` kollidieren. Darum startest du Postgres selbst mit `docker run` (T3). Wie es mit Compose geht, steht in 4.2.3 von [4.md](./4.md).

### Spielregeln

1. Zuerst die **Vorhersage** ins Feld ✍️ schreiben, *dann* den Befehl ausführen.
2. Mit **✔ Check** vergleichen. Die erwarteten Ausgaben stammen aus einem Probelauf; Zeiten, IDs und IP-Adressen können bei dir anders sein.
3. Erst danach die Lösung aufklappen.
4. Oft committen: `git add -A && git commit -m "Training T3" && git push`

### Stolpersteine

- **Ordner:** In VS Code *File → Open Folder → `/home/dev/school/m324/m324-grade-compass-uebung`*. Alle Befehle laufen im Repo-Root dieses Ordners (zsh, damit `mvn`/`java` über SDKMAN da sind).
- **Startzeit:** Spring Boot braucht ca. 10 s. Direkt nach `docker run` antwortet `curl` noch nicht. Warten mit:
  `until curl -s localhost:18080/api/subjects; do sleep 1; done; echo`
- **`until curl` hört nach 30 s nicht auf?** Mit Ctrl+C abbrechen, dann `docker ps -a`. Steht der Backend-Container auf `Exited`, zeigt `docker logs uebung-backend 2>&1 | tail -20` den Grund (oft war die DB noch nicht bereit → `docker start uebung-backend`).
- **`make test` ist rot** wegen `SubjectServiceTest.update_duplicateName_throwsConflictAndDoesNotSave` (Aufgabe 2, nicht Teil von Aufgabe 4). Für Aufgabe 4 reicht `cd backend && mvn test -Dtest=CreateSubjectRequestTest`.
- **Browser:** VS Code läuft über Remote-SSH, darum muss der Port (z. B. 18080) im Tab *Ports* weitergeleitet werden. Für alle Checks reicht `curl`.

---

## T1 Abhängigkeiten nachweisen (4.1.2 – 4.1.4)

**Ziel:** Mit Maven zeigen statt raten: direkte vs. transitive Abhängigkeiten, woher die Version kommt, welcher Scope.

✍️ Vorhersage – wie viele direkte Abhängigkeiten hat das `pom.xml`? Welche Version hat der PostgreSQL-Treiber und wo steht sie?

> Deine Vorhersage:

```bash
cd backend
mvn -q dependency:tree -DoutputFile=/tmp/tree.txt
echo "direkt: $(grep -cE '^[+\\]- ' /tmp/tree.txt)  total: $(tail -n +2 /tmp/tree.txt | wc -l)"
mvn -q help:evaluate -Dexpression=postgresql.version -DforceStdout; echo
mvn dependency:tree -Dincludes=com.h2database,org.postgresql | grep -E "h2|postgresql"
cd ..
```

**✔ Check**

```text
direkt: 8  total: 116
42.7.14
[INFO] +- org.postgresql:postgresql:jar:42.7.14:runtime
[INFO] \- com.h2database:h2:jar:2.4.240:test
```

<details>
<summary>Lösung</summary>

- **8 direkte** Abhängigkeiten (erste Ebene im Baum), **116 Libraries** im ganzen Baum. Der Rest sind **transitive** Abhängigkeiten (Abhängigkeiten der Abhängigkeiten).
- **Keine Version im `pom.xml`:** Das Projekt erbt von `spring-boot-starter-parent`. Dessen Parent `spring-boot-dependencies` ist eine **BOM** (Bill of Materials), die für jede Library eine getestete, zusammenpassende Version festlegt (z. B. Property `postgresql.version` = `42.7.14`). Wird Spring Boot aktualisiert, ziehen alle Versionen mit.
- **`runtime`:** Zum Kompilieren nicht nötig (der Code benutzt nur JPA/JDBC-Interfaces), beim Ausführen schon → kommt ins JAR. Darum PostgreSQL-Treiber.
- **`test`:** Nur beim Kompilieren und Ausführen der Tests, **nicht** im JAR. Darum H2: Die Tests laufen mit einer In-Memory-DB, ohne dass ein Postgres laufen muss.

</details>

---

## T2 Multi-Stage `Dockerfile` (4.2.1)

**Ziel:** Das Backend-Image selbst schreiben.

**Aufgabe:** Erstelle `backend/Dockerfile` und `backend/.dockerignore`.

- Teil 1 (`AS build`): Image `maven:3-eclipse-temurin-25`. Zuerst **nur** `pom.xml` kopieren und `mvn -B dependency:go-offline` ausführen, danach `src` kopieren und `mvn -B package -DskipTests`.
- Teil 2: Image `eclipse-temurin:25-jre`, nur das JAR aus Teil 1 kopieren, Port 8080, mit `java -jar` starten.
- `.dockerignore`: `target` ausschliessen.

Tipp: Die Java-Version steht im `pom.xml` (`<java.version>25</java.version>`).

✍️ Vorhersage – welches Image ist grösser, `build` oder das fertige? Ist H2 im JAR?

> Deine Vorhersage:

```bash
docker build -t uebung-backend backend
docker build --target build -t uebung-backend:build backend
docker images uebung-backend --format '{{.Tag}} {{.Size}}'
docker run --rm uebung-backend sh -c 'java -version 2>&1 | head -1; for t in javac mvn; do command -v $t >/dev/null && echo "$t: da" || echo "$t: fehlt"; done'
docker run --rm uebung-backend:build sh -c 'jar tf target/*.jar | grep -c "BOOT-INF/lib/.*\.jar$"; jar tf target/*.jar | grep -E "lib/(postgresql|h2)"'
```

**✔ Check** (erster Build ca. 35 s, ca. 890 Downloads von Maven Central)

```text
latest 599MB
build 1.11GB
openjdk version "25.0.5.1" 2026-11-17 LTS
javac: fehlt
mvn: fehlt
71
BOOT-INF/lib/postgresql-42.7.14.jar
```

<details>
<summary>Lösung</summary>

`git show main:backend/Dockerfile` und `git show main:backend/.dockerignore`

```dockerfile
FROM maven:3-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
```

```text
target
```

**Weshalb zwei Teile?**
- Zum **Bauen** braucht es JDK und Maven samt Maven-Cache (1.11 GB), zum **Ausführen** nur eine JRE und das JAR (599 MB).
- Weniger Software im fertigen Image heisst: kleiner, schneller zu verteilen, weniger Angriffsfläche (kein Compiler, kein Maven).
- Der Build ist reproduzierbar, weil er nicht vom Rechner des Entwicklers abhängt.

**Weshalb ist H2 nicht im JAR?** Scope `test`. Im JAR landen nur `compile`- und `runtime`-Abhängigkeiten (71 Libraries statt 116).

**Weshalb `.dockerignore` mit `target`?** Ein lokales `target` würde unnötig mit in den Build-Kontext geschickt. Ein altes JAR darin könnte sogar im Image landen, wenn man es falsch kopiert.

</details>

---

## T3 Backend und Datenbank im selben Netzwerk (4.2.3)

**Ziel:** Verstehen, wie Container sich finden. Zuerst von Hand, in T4 dann mit `make`.

**Aufgabe:**
1. Erstelle das Netzwerk `uebung-net`.
2. Starte `postgres:18` als `uebung-postgres` in diesem Netzwerk (`POSTGRES_DB=gcdb`, `POSTGRES_USER=gcdbUser`, `POSTGRES_PASSWORD=uebung`).
3. Starte `uebung-backend` als Container `uebung-backend` im selben Netzwerk auf Port 18080. Welche Umgebungsvariablen braucht es? → `grep -n DB_ backend/src/main/resources/application.properties`

**✔ Check**

```bash
until curl -s localhost:18080/api/subjects; do sleep 1; done; echo
```

```text
[]
```

**Wie findet das Backend die DB?** Docker hat einen eingebauten DNS:

```bash
docker run --rm --network uebung-net alpine getent hosts uebung-postgres; echo "exit=$?"
docker run --rm alpine getent hosts uebung-postgres; echo "exit=$?"
```

```text
172.18.0.2        uebung-postgres  uebung-postgres
exit=0
exit=2
```

### T3b Fehlerbilder vorhersagen

Starte drei falsche Varianten und sag **vorher**, was passiert und welche Meldung im Log steht:

| Container | Fehler | ✍️ Deine Vorhersage |
|---|---|---|
| `u-test1` | kein `DB_URL` (also `localhost`) | |
| `u-test2` | richtiges `DB_URL`, aber kein `--network` | |
| `u-test3` | richtiges Netzwerk, aber kein `DB_PASSWORD` | |

```bash
docker run -d --name u-test1 --network uebung-net -e DB_PASSWORD=uebung uebung-backend
docker run -d --name u-test2 -e DB_URL=jdbc:postgresql://uebung-postgres:5432/gcdb -e DB_PASSWORD=uebung uebung-backend
docker run -d --name u-test3 --network uebung-net -e DB_URL=jdbc:postgresql://uebung-postgres:5432/gcdb uebung-backend
sleep 25
docker ps -a --filter name=u-test --format '{{.Names}} {{.Status}}'
docker logs u-test1 2>&1 | grep -m1 -o "Connection to localhost:5432 refused"
docker logs u-test2 2>&1 | grep -m1 -o "UnknownHostException: uebung-postgres"
docker logs u-test3 2>&1 | grep -m1 -o 'password authentication failed for user "gcdbUser"'
docker rm -f u-test1 u-test2 u-test3
```

**✔ Check:** Alle drei laufen zuerst ca. 10–12 s, danach `Exited (1)`. Mit `sleep 10` sind sie noch "Up"!

```text
Connection to localhost:5432 refused
UnknownHostException: uebung-postgres
password authentication failed for user "gcdbUser"
```

<details>
<summary>Lösung</summary>

```bash
docker network create uebung-net
docker run -d --name uebung-postgres --network uebung-net \
  -e POSTGRES_DB=gcdb -e POSTGRES_USER=gcdbUser -e POSTGRES_PASSWORD=uebung postgres:18
docker run -d --name uebung-backend --network uebung-net -p 18080:8080 \
  -e DB_URL=jdbc:postgresql://uebung-postgres:5432/gcdb -e DB_PASSWORD=uebung uebung-backend
```

- **`localhost` im Container** ist der Container selbst. Im Backend-Container läuft kein Postgres → `Connection refused`.
- **Containername als Hostname** funktioniert nur in einem **eigenen** Netzwerk. Im Standard-Netzwerk `bridge` gibt es keinen DNS für Namen → `UnknownHostException`.
- **Mit Compose** (Aufgabe 4.2.3 auf `main`) erstellt Docker das Netzwerk automatisch: `<Ordnername>_default`, also **`backend_default`**. Der Hostname ist dort der Service- bzw. Containername (`grade-compass-postgres`).
- `-p 18080:8080` = Host-Port : Container-Port. Die DB braucht keinen `-p`, weil nur das Backend sie im Netzwerk erreichen muss.

</details>

---

## T4 Makefile-Targets (4.2.2 + 4.2.5)

**Ziel:** Die Befehle aus T2/T3 wiederholbar machen.

**Aufgabe:** Ergänze das `Makefile` mit:

| Target | Anforderung |
|---|---|
| `docker` | baut `uebung-backend:$(TAG)`, `TAG` ist standardmässig `latest` |
| `db` | erstellt das Netzwerk, falls es fehlt, startet `uebung-postgres` nur, falls er nicht schon läuft (Daten bleiben), und wartet, bis Postgres bereit ist |
| `run` | startet ein **bestehendes** Image (baut **nicht**!), Variablen `NAME` (Standard `uebung-backend`) und `PORT` (Standard `18080`) |
| `docker-clean` | entfernt Container, Netzwerk und alle `uebung-backend`-Images; zweimal hintereinander ohne Fehler |

✍️ Vorhersage – weshalb darf `run` **nicht** von `docker` abhängen? (Denk an T6, wenn du `make run TAG=1.0` machst, nachdem der Code schon geändert ist.)

> Deine Vorhersage:

**✔ Check**

```bash
make -n docker TAG=2.0
docker rm -f uebung-backend uebung-postgres; docker network rm uebung-net
make docker && make run
until curl -s localhost:18080/api/subjects; do sleep 1; done; echo
make run; echo "exit $?"
```

```text
docker build -t uebung-backend:2.0 backend
...
[]
...
exit 0
```

`docker-clean` testest du erst am Schluss (T10), sonst sind die Images weg.

<details>
<summary>Lösung</summary>

Unten ans `Makefile` anhängen (Einrückung mit **Tab**, sonst `*** missing separator`):

```makefile
IMAGE ?= uebung-backend
TAG ?= latest
NAME ?= uebung-backend
PORT ?= 18080
NETWORK ?= uebung-net
DB_PW ?= uebung

.PHONY: docker db run docker-clean

docker:
	docker build -t $(IMAGE):$(TAG) backend

db:
	docker network inspect $(NETWORK) >/dev/null 2>&1 || docker network create $(NETWORK)
	docker start uebung-postgres >/dev/null 2>&1 || docker run -d --name uebung-postgres --network $(NETWORK) \
		-e POSTGRES_DB=gcdb -e POSTGRES_USER=gcdbUser -e POSTGRES_PASSWORD=$(DB_PW) postgres:18
	until docker exec uebung-postgres pg_isready -q -h 127.0.0.1 -U gcdbUser; do sleep 1; done

run: db
	docker rm -f $(NAME)
	docker run -d --name $(NAME) --network $(NETWORK) -p $(PORT):8080 \
		-e DB_URL=jdbc:postgresql://uebung-postgres:5432/gcdb -e DB_PASSWORD=$(DB_PW) $(IMAGE):$(TAG)

docker-clean:
	docker rm -f uebung-backend uebung-backend-2 uebung-postgres
	-docker network rm $(NETWORK)
	docker images -q $(IMAGE) | sort -u | xargs -r docker rmi -f
```

- `?=` setzt nur einen Standardwert, `make docker TAG=2.0` überschreibt ihn.
- **`run` ohne `docker`:** Sonst würde `make run TAG=1.0` das Image `1.0` aus dem **aktuellen** Code (schon 50 Zeichen) neu bauen und die alte Version überschreiben. Ein Rollback wäre dann unmöglich.
- `pg_isready -h 127.0.0.1`: Beim ersten Start läuft Postgres kurz nur intern (Initialisierung). Über TCP gefragt, sagt es erst "bereit", wenn es wirklich bereit ist.
- `docker rm -f` meldet bei fehlenden Containern nur einen Hinweis (Exit 0). `docker network rm` bricht bei fehlendem Netzwerk ab, darum das `-` davor (make ignoriert den Fehler). `docker rmi` mit fehlendem Image bricht ebenfalls ab, darum nur vorhandene IDs über `xargs -r`.
- Auf `main` heisst das Image `grade-compass-backend:latest` (4.2.2), siehe `git show main:Makefile`.

</details>

---

## T5 Layer-Cache vorhersagen (4.2.4)

**Ziel:** Verstehen, warum die Reihenfolge im `Dockerfile` zählt. **Sehr prüfungsrelevant.**

Messen jeweils mit:

```bash
time (docker build --progress=plain -t uebung-backend backend > /tmp/build.log 2>&1)
grep -E "^#[0-9]+ (\[(build|stage)|CACHED)" /tmp/build.log
grep -c "Downloaded from" /tmp/build.log
```

Steht unter einem Schritt `#N CACHED`, kommt er aus dem Zwischenspeicher.

| # | Änderung | ✍️ Welche Schritte CACHED? Wie viele Downloads? |
|---|---|---|
| a | Kommentar in `GradeCompassApplication.java` | |
| b | Nur `backend/docs/subject-dokumentation.md` ändern | |
| c | `spring-boot-starter-actuator` im `pom.xml` ergänzen | |
| d | Schlechtes Dockerfile (siehe unten), 2× bauen mit einer Kommentar-Änderung dazwischen | |

Für **d** ein zweites Dockerfile `backend/Dockerfile.schlecht` erstellen, das alles auf einmal kopiert:

```dockerfile
FROM maven:3-eclipse-temurin-25 AS build
WORKDIR /app
COPY . .
RUN mvn -B package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
```

```bash
docker build --progress=plain -f backend/Dockerfile.schlecht -t uebung-schlecht backend > /tmp/build.log 2>&1; grep -c "Downloaded from" /tmp/build.log
```

<details>
<summary>✔ Check: gemessene Werte</summary>

| # | Änderung | Schritte (build 2–6) | Downloads | Dauer |
|---|---|---|---|---|
| a | Kommentar Java | 2–4 CACHED, 5 `COPY src` und 6 `package` neu | 0 | ca. 10–12 s |
| b | nur `docs/` | **alles** CACHED | 0 | < 1 s |
| c | Actuator im pom | 2 CACHED, ab 3 `COPY pom.xml` alles neu | 887 | ca. 28 s |
| d | schlechtes Dockerfile | `COPY . .` und `package` jedes Mal neu | **573 bei jeder Code-Änderung** | ca. 15 s |

</details>

<details>
<summary>Lösung / Erklärung</summary>

- Docker baut Schicht für Schicht. Ändert sich der Input einer Schicht (bei `COPY` der Inhalt der kopierten Files), werden **diese und alle folgenden** Schichten neu gebaut.
- **a:** `pom.xml` ist gleich → `go-offline` (alle Abhängigkeiten) kommt aus dem Cache. Nur Code kopieren und kompilieren.
- **b:** `docs/` wird nie kopiert → kein `COPY` hat einen anderen Input → alles aus dem Cache (auch wenn `docs/` im Build-Kontext ist).
- **c:** `pom.xml` hat sich geändert → ab `COPY pom.xml` alles neu, inkl. Download aller Abhängigkeiten.
- **d:** Mit `COPY . .` vor dem Build ändert **jede** Code-Änderung den Input → Abhängigkeiten werden jedes Mal neu geladen.
- **Regel:** Was sich selten ändert (Abhängigkeiten), kommt zuerst; was sich oft ändert (Code), kommt zuletzt.
- Warum d "nur" 573 statt 887 Downloads hat: `go-offline` lädt auf Vorrat auch Plugins, die `package` gar nicht braucht. Bei langsamem Netz ist der Unterschied zwischen a und d viel grösser als hier.

Danach aufräumen und wieder den Stand mit 100 Zeichen bauen (Grundlage für T6):

```bash
git restore backend/src backend/docs backend/pom.xml   # oder Actuator behalten, wie auf main
rm backend/Dockerfile.schlecht; docker rmi uebung-schlecht
make docker
```

</details>

---

## T6 Version 1.0 und 2.0 nebeneinander (4.3.1 + 4.3.2)

**Ziel:** Zwei Versionen gleichzeitig betreiben und den Unterschied sehen.

1. **Zuerst** das aktuelle Image als `1.0` markieren (Code noch mit 100 Zeichen!):

   ```bash
   docker tag uebung-backend:latest uebung-backend:1.0
   ```

2. In `backend/src/main/java/ch/bbzw/m324/grade_compass/subject/CreateSubjectRequest.java` `@Size(max = 100)` → `@Size(max = 50)`.

   ✍️ Vorhersage – welcher Test schlägt fehl?

   > Deine Vorhersage:

   ```bash
   cd backend && mvn test -Dtest=CreateSubjectRequestTest; cd ..
   ```

   **✔ Check**

   ```text
   [ERROR] Tests run: 9, Failures: 1, Errors: 0, Skipped: 0
   ... CreateSubjectRequestTest.nameWithExactly100Characters_isAccepted ... FAILURE!
   ```

   Passe die Tests an die neue Regel an (Grenzwerte 50 ok / 51 abgelehnt), bis `mvn -q test -Dtest=CreateSubjectRequestTest` keine Fehler mehr zeigt.

3. `2.0` bauen und beide starten:

   ```bash
   make docker TAG=2.0
   make run TAG=1.0
   make run TAG=2.0 NAME=uebung-backend-2 PORT=18081
   until curl -s localhost:18081/api/subjects; do sleep 1; done; echo
   docker ps --filter name=uebung --format '{{.Names}}  {{.Image}}  {{.Ports}}'
   ```

4. An beide ein Fach mit 60 Zeichen schicken (funktioniert in zsh und bash):

   ✍️ Vorhersage – was antworten 1.0 und 2.0? Was zeigt danach `GET` auf **2.0**?

   > Deine Vorhersage:

   ```bash
   N60=$(printf 'A%.0s' {1..60})
   for p in 18080 18081; do curl -s -w " HTTP %{http_code}\n" -H 'Content-Type: application/json' -d "{\"name\":\"$N60\"}" localhost:$p/api/subjects; done
   curl -s localhost:18081/api/subjects; echo
   ```

**✔ Check**

```text
uebung-backend-2  uebung-backend:2.0  0.0.0.0:18081->8080/tcp, [::]:18081->8080/tcp
uebung-backend  uebung-backend:1.0  0.0.0.0:18080->8080/tcp, [::]:18080->8080/tcp
uebung-postgres  postgres:18  5432/tcp
{"id":1,"name":"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"} HTTP 201
{"timestamp":"...","status":400,"error":"Bad Request","path":"/api/subjects"} HTTP 400
[{"id":1,"name":"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"}]
```

Schickst du den gleichen Namen nochmals an 1.0, kommt `409` (Name existiert schon).

<details>
<summary>Lösung</summary>

- Test-Anpassung: `git show main:backend/src/test/java/ch/bbzw/m324/grade_compass/subject/CreateSubjectRequestTest.java` (100 → 50, 101 → 51, Testnamen angepasst). Der `SubjectControllerTest` mit 101 Zeichen bleibt grün (101 > 50), ist aber ungenau; auf `main` wurde er auf 51 angepasst.
- **Feststellung:** Gleicher Request, unterschiedliche Antwort: 1.0 speichert (201), 2.0 lehnt ab (400). Beide teilen die gleiche DB: 2.0 **liest** problemlos einen Namen, den es selbst nie annehmen würde. Die Daten halten sich also nicht an die neue Regel.
- Images sind **unveränderlich**; jede Version hat einen eigenen Tag. Darum laufen beide gleichzeitig, solange Name und Port verschieden sind.

</details>

---

## T7 Rollback (4.3.3)

**Ziel:** Zurück auf 1.0, wenn 2.0 Probleme macht.

Simuliere das Deployment: 2.0 ersetzt 1.0 auf Port 18080.

```bash
docker rm -f uebung-backend-2
make run TAG=2.0
until curl -s localhost:18080/api/subjects; do sleep 1; done; echo
docker ps --filter name=uebung-backend --format '{{.Names}} {{.Image}}'
```

✍️ Wie gehst du zurück? Wie lange dauert es?

> Deine Antwort:

**✔ Check** nach dem Rollback

```text
uebung-backend uebung-backend:1.0
```

Dauer bis die API wieder antwortet: ca. 10 s.

<details>
<summary>Lösung</summary>

```bash
make run TAG=1.0      # = docker rm -f uebung-backend + docker run ... uebung-backend:1.0
```

- Das Image `1.0` ist noch da (`docker images uebung-backend`) → kein Neubau, nur Container austauschen.
- Darum **nie nur `latest`** verwenden: Wird `latest` überschrieben, gibt es keinen alten Stand mehr zum Zurückgehen.
- Den Code zurücksetzen geht mit `git revert <commit>` (neuer Commit, Geschichte bleibt) und danach neu bauen. Das dauert aber länger.
- **Die DB wird beim Rollback nicht zurückgesetzt!** Daten oder Schema-Änderungen von 2.0 bleiben → T8.

</details>

---

## T8 Schema-Änderung bei gemeinsamer DB (4.3.4)

**Ziel:** Sehen, was passiert, wenn eine neue Version das Schema ändert und die alte noch läuft.

✍️ Vorhersage – was antwortet 1.0, wenn 2.0 die Spalte `name` in `title` umbenennt? Hilft ein Neustart von 1.0?

> Deine Vorhersage:

```bash
docker exec uebung-postgres psql -U gcdbUser -d gcdb -c 'ALTER TABLE subject RENAME COLUMN name TO title;'
curl -s -w " HTTP %{http_code}\n" localhost:18080/api/subjects
docker logs uebung-backend 2>&1 | grep -m1 -oE 'column [a-z0-9_.]+ does not exist'

make run TAG=1.0
sleep 15
docker logs uebung-backend 2>&1 | grep -m1 -oE '(alter table[^"]{0,60}|contains null values)'
curl -s -w " HTTP %{http_code}\n" localhost:18080/api/subjects
```

**✔ Check**

```text
ALTER TABLE
{"timestamp":"...","status":500,"error":"Internal Server Error","path":"/api/subjects"} HTTP 500
column s1_0.name does not exist
alter table if exists subject add column name varchar(255) not null
contains null values
{"timestamp":"...","status":500,"error":"Internal Server Error","path":"/api/subjects"} HTTP 500
```

Wiederherstellen (danach sofort wieder 200, ohne Neustart):

```bash
docker exec uebung-postgres psql -U gcdbUser -d gcdb -c 'ALTER TABLE subject RENAME COLUMN title TO name;'
curl -s -w " HTTP %{http_code}\n" localhost:18080/api/subjects
```

<details>
<summary>Lösung</summary>

- Version 1.0 erwartet die Spalte `name` → jede Abfrage scheitert (500).
- Ein Neustart hilft nicht: Hibernate (`ddl-auto=update`) will die fehlende Spalte `name` als `not null` ergänzen. Das geht nicht, weil bestehende Zeilen dort leer wären (`contains null values`). Die App startet trotzdem, liefert aber weiter 500.
- **Ein Rollback des Codes setzt das Schema nicht zurück.** Alte und neue Version müssen mit dem gleichen Schema umgehen können.
- Lösungsansätze:
  - **Abwärtskompatibel ändern (Expand/Contract):** zuerst neue Spalte *zusätzlich* anlegen, beide befüllen, erst wenn keine alte Version mehr läuft die alte Spalte entfernen.
  - Schema über **Migrationen** versionieren (z. B. Flyway, Liquibase) statt `ddl-auto=update`.
  - Vor einer Migration Backup erstellen.
- Auch die neue 50er-Regel als DB-Constraint (`varchar(50)`) würde scheitern, sobald 1.0 schon längere Namen gespeichert hat (`value too long for type character varying(50)`, gemessen in 4.md).

</details>

---

## T9 Zusatz: Frontend als Container (4.4, optional)

**Ziel:** Gleiches Prinzip mit Node und nginx.

**Aufgabe:** Erstelle `frontend/Dockerfile` (Teil 1 `node:26`: `package.json` + `package-lock.json` kopieren, `npm ci`, Rest kopieren, `npm run build`; Teil 2 `nginx:1.31-alpine` mit `dist/frontend/browser`) und `frontend/.dockerignore`.

```bash
docker build -t uebung-frontend frontend
docker run -d --name uebung-frontend -p 14200:80 uebung-frontend
for u in / /subjects /grades; do echo "$u -> $(curl -s -o /dev/null -w '%{http_code}' localhost:14200$u)"; done
```

✍️ Vorhersage – welche Status-Codes kommen *ohne* eigene nginx-Konfiguration?

> Deine Vorhersage:

**✔ Check** (Build ca. 70–95 s, Image 103 MB)

| URL | ohne `nginx.conf` | mit `nginx.conf` (`try_files`) |
|---|---|---|
| `/` | 200 | 200 |
| `/subjects` | **404** | 200 |
| `/grades` | **404** | 200 |

Die App im Browser zeigt hier keine Daten: Die API-URL `localhost:8080` ist ins JavaScript eingebaut, und das Backend erlaubt (CORS) nur `http://localhost:4200`. Darum nur der `curl`-Check.

<details>
<summary>Lösung</summary>

`git show main:frontend/Dockerfile`, `git show main:frontend/nginx.conf`, `git show main:frontend/.dockerignore` (`node_modules`, `dist`, `.angular`, `.vscode`)

- `/subjects` ist keine Datei, sondern eine Angular-Route. nginx sucht standardmässig eine Datei → 404. Mit `try_files $uri $uri/ /index.html;` liefert nginx `index.html` aus und der Angular-Router übernimmt.
- `npm ci` statt `npm install`: installiert exakt nach `package-lock.json`, bricht ab, wenn Lock-File und `package.json` nicht zusammenpassen → reproduzierbar.
- Im fertigen Image ist kein Node mehr, nur nginx und statische Dateien.

```bash
docker rm -f uebung-frontend; docker rmi uebung-frontend
```

</details>

---

## T10 Aufräumen (4.2.5)

```bash
make docker-clean; echo "exit $?"
make docker-clean; echo "exit $?"
docker ps -a --filter name=uebung --format '{{.Names}}'
docker images 'uebung-*' --format '{{.Repository}}:{{.Tag}}'
```

**✔ Check:** Beide Male `exit 0`, beim zweiten Mal mit `make: [...] Error 1 (ignored)` für das fehlende Netzwerk. Die letzten zwei Befehle geben nichts mehr aus.

---

## Prüfungsfragen

<details>
<summary>1. Was passiert, wenn beim Ausführen eine Abhängigkeit fehlt?</summary>

Programm startet nicht oder stürzt erst später ab, wenn die Klasse gebraucht wird (z. B. `ClassNotFoundException`, `NoClassDefFoundError`, "module not found"). Bei falscher Version: Methoden fehlen, anderes Verhalten, Sicherheitslücken. Typisch: "Bei mir läuft's" → Container und Lock-Files lösen das.

</details>

<details>
<summary>2. Direkte vs. transitive Abhängigkeit?</summary>

Direkt = im eigenen `pom.xml`/`package.json` deklariert (Backend: 8). Transitiv = Abhängigkeit einer Abhängigkeit, kommt automatisch mit (Backend total: 116).

</details>

<details>
<summary>3. Woher kommen die Versionen im pom.xml, wenn keine dasteht?</summary>

Aus dem Parent `spring-boot-starter-parent` → BOM `spring-boot-dependencies` (`dependencyManagement`). Vorteil: getestete, zusammenpassende Versionen, Update an einer Stelle.

</details>

<details>
<summary>4. Maven-Scopes compile, runtime, test, provided?</summary>

| Scope | Kompilieren | Tests | Im JAR / Laufzeit |
|---|---|---|---|
| `compile` (Standard) | ✔ | ✔ | ✔ |
| `runtime` | ✘ | ✔ | ✔ (z. B. PostgreSQL-Treiber) |
| `test` | nur Tests | ✔ | ✘ (z. B. H2, JUnit) |
| `provided` | ✔ | ✔ | ✘ (stellt die Umgebung bereit, z. B. Servlet-API) |

</details>

<details>
<summary>5. dependencies vs. devDependencies? ^ vs. ~?</summary>

`dependencies` braucht die App zur Laufzeit (Angular, rxjs). `devDependencies` nur zum Entwickeln/Bauen/Testen (Angular CLI, TypeScript, ESLint, Vitest); die landen nicht im fertigen Bundle.
`^22.2.0` = alle `22.x.x` ab 22.2.0 (Minor + Patch). `~7.8.0` = nur `7.8.x` (nur Patch).

</details>

<details>
<summary>6. Wozu package-lock.json? npm install vs. npm ci?</summary>

Lock-File hält die **exakten** Versionen des ganzen Baums fest (inkl. transitive) → alle installieren dasselbe. `npm install` löst Bereiche (`^`, `~`) auf und kann das Lock-File ändern. `npm ci` löscht `node_modules`, installiert exakt nach Lock-File, ändert es nie und bricht bei Abweichungen ab → für Builds/CI/Docker.

</details>

<details>
<summary>7. Weshalb ein Multi-Stage-Dockerfile?</summary>

Build-Werkzeuge (JDK, Maven, Node) nur im ersten Teil. Das fertige Image enthält nur Laufzeit + Artefakt → kleiner (599 MB statt 1.11 GB), sicherer, schneller verteilt.

</details>

<details>
<summary>8. Weshalb zuerst nur pom.xml / package.json kopieren?</summary>

Layer-Cache: Solange sich die Abhängigkeits-Files nicht ändern, kommt der teure Download-Schritt aus dem Cache. Code-Änderungen bauen nur die letzten Schichten neu (Kommentar: 0 Downloads statt 887).

</details>

<details>
<summary>9. Weshalb darf in DB_URL nicht localhost stehen?</summary>

`localhost` ist im Container der Container selbst. Die DB läuft in einem anderen Container → über dessen Namen im gemeinsamen Netzwerk ansprechen (Compose: Netzwerk `backend_default`).

</details>

<details>
<summary>10. Wie betreibt man zwei Versionen gleichzeitig, und wie macht man einen Rollback?</summary>

Jede Version eigener Image-Tag (`1.0`, `2.0`), Container mit eigenem Namen und Host-Port. Rollback = Container mit altem Tag starten; geht nur, wenn das alte Image noch existiert (nicht nur `latest`).

</details>

<details>
<summary>11. Welche Probleme gibt es, wenn beide Versionen dieselbe DB nutzen?</summary>

Schema-Änderungen (Spalte umbenennen/löschen) brechen die alte Version (500, `column does not exist`). Neue Regeln gelten nicht für alte Daten (60er Namen trotz Limit 50). Rollback des Codes setzt die DB nicht zurück. → abwärtskompatible Migrationen (Expand/Contract), Flyway/Liquibase, Backup.

</details>

---

## Spickzettel (für die A4-Zusammenfassung)

**Begriffe**
- Direkte / transitive Abhängigkeit · BOM (Parent legt Versionen fest) · Lock-File (exakte Versionen)
- Scopes: `compile` überall · `runtime` nicht zum Kompilieren, aber im JAR · `test` nur Tests, nicht im JAR · `provided` nicht im JAR
- `^1.2.3` → `1.x.x` · `~1.2.3` → `1.2.x` · `npm ci` = exakt nach Lock-File, für CI/Docker

**Dockerfile-Muster**
```dockerfile
FROM <build-image> AS build
COPY <abhängigkeits-files> .     # pom.xml | package*.json
RUN <abhängigkeiten laden>       # mvn dependency:go-offline | npm ci
COPY <code> .
RUN <bauen>                      # mvn package | npm run build
FROM <laufzeit-image>            # JRE | nginx
COPY --from=build <artefakt> .
```

**Befehle**
| Zweck | Befehl |
|---|---|
| Image bauen / Tag | `docker build -t name:tag ordner` · `docker tag name:latest name:1.0` |
| Nur Stage bauen | `docker build --target build ...` |
| Netzwerk | `docker network create netz` · Container mit `--network netz` |
| Starten | `docker run -d --name n --network netz -p HOST:CONTAINER -e VAR=wert image:tag` |
| Logs / Status | `docker logs n` · `docker ps -a` · `docker images` |
| Weg damit | `docker rm -f n` · `docker rmi image:tag` · `docker network rm netz` |
| Maven-Baum | `mvn dependency:tree` · `mvn help:evaluate -Dexpression=x.version -DforceStdout` |

**Zahlen aus dem Projekt**
- Backend: 8 direkte, 116 Libraries total, 71 im JAR · PostgreSQL 42.7.14 (`runtime`), H2 2.4.240 (`test`)
- Image 599 MB (JRE) vs. Build-Stage 1.11 GB · Frontend-Image 103 MB (nginx)
- Cache: Kommentar → 0 Downloads; `pom.xml` geändert → 887 Downloads

**Merksätze**
- `localhost` im Container = der Container selbst.
- Namen auflösen geht nur im eigenen Netzwerk (Compose: `<ordner>_default`).
- Selten Änderndes zuerst ins Dockerfile, Code zuletzt.
- Nie nur `latest`, sonst kein Rollback.
- Code-Rollback ≠ DB-Rollback → Schema abwärtskompatibel ändern.
