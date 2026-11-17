# Scénario de démonstration (5 minutes)

Deux versions du même scénario :

- **Version A** : toute la plateforme dans Docker, avec Keycloak. C'est la version visée.
- **Version B** (plan B) : les services sont lancés avec Maven et la Gateway avec `SECURITY_ENABLED=false`. À utiliser si Docker, Keycloak ou les jetons posent problème le jour J.

Les étapes portent les mêmes numéros dans les deux versions. Pour chaque étape, le tableau donne la requête Postman à lancer, le résultat attendu et la phrase à dire au jury.

> ⚠️ La version A n'a pas encore été exécutée de bout en bout (Docker n'était pas installé sur le poste de développement). La répéter au moins une fois avant la soutenance.

---

## Checklist de préparation

### La veille

- [ ] `mvnw clean test` dans chaque service : les 73 tests passent.
- [ ] Version A : construire les images une première fois. La première construction prend plusieurs minutes.
- [ ] Importer dans Postman les deux collections de [docs/](.) :
  - `XYZ-Smart-Marina-Gateway` pour le scénario ;
  - `XYZ-Smart-Marina` pour l'appel direct du plan B.
- [ ] Faire une répétition complète, chronomètre en main.

### Le jour J : ports utilisés

| Composant | Port | Version A | Version B |
| --- | --- | --- | --- |
| api-gateway | 8080 | exposé | local |
| Keycloak | 8180 | exposé | non utilisé |
| port-service | 8081 | interne Docker | local |
| pedestal-service | 8082 | interne Docker | local |
| consumption-service | 8083 | interne Docker | local |
| notification-service | 8084 | interne Docker | local |
| PostgreSQL | 5433 à 5436 | conteneurs | conteneurs ou PostgreSQL local |
| Kafka | 9092 | conteneur | conteneur ou Kafka local |

Vérifier qu'aucun autre programme n'occupe ces ports (`netstat -ano | findstr :8080`).

### Version A : démarrage (environ 10 minutes avant)

1. Repartir de bases vides, pour que le premier port créé ait l'identifiant **1** (c'est le `port_id` de `manager.port1`) :
   ```powershell
   docker compose -f docker/docker-compose.yml --profile apps down -v
   ```
   `-v` supprime les données de démonstration précédentes.
2. Tout démarrer :
   ```powershell
   docker compose -f docker/docker-compose.yml --profile apps up -d --build
   ```
3. Attendre que tout soit prêt :
   - `docker compose -f docker/docker-compose.yml ps` : les bases sont `healthy` ;
   - `http://localhost:8180` affiche Keycloak (30 à 60 s) ;
   - `GET http://localhost:8080/actuator/health` renvoie `UP`.
4. Dans Postman, lancer **0 - Auth › Jeton ADMIN**. Le jeton est stocké dans `accessToken`.

### Version B : démarrage (environ 10 minutes avant)

1. Démarrer PostgreSQL et Kafka, sans Keycloak :
   ```powershell
   docker compose -f docker/docker-compose.yml up -d postgres-port postgres-pedestal postgres-consumption postgres-notification kafka
   ```
   Sans Docker, il faut un PostgreSQL local avec les 4 bases (`portdb`/`port`/`port123`, `pedestaldb`/`pedestal`/`pedestal123`, `consumptiondb`/`consumption`/`consumption123`, `notificationdb`/`notification`/`notification123`) sur les ports 5433 à 5436, ou changer `DB_URL`. Il faut aussi un Kafka sur `localhost:9092`.
2. Lancer les services dans cet ordre, un terminal chacun :
   ```powershell
   cd port-service;         .\mvnw.cmd spring-boot:run
   cd pedestal-service;     .\mvnw.cmd spring-boot:run
   cd consumption-service;  .\mvnw.cmd spring-boot:run
   cd notification-service; .\mvnw.cmd spring-boot:run
   cd api-gateway; $env:SECURITY_ENABLED='false'; .\mvnw.cmd spring-boot:run
   ```
3. Vérifier `GET http://localhost:8080/actuator/health` : `UP`.
4. Pour repartir de bases vides, supprimer les volumes avant l'étape 1 (`docker compose -f docker/docker-compose.yml down -v`).

En version B, le dossier **0 - Auth** est inutile. Les autres requêtes de la collection Gateway fonctionnent sans jeton.

### Données à créer avant le jury

Aucune. Le scénario crée lui-même le port, la borne et les consommations, et Postman enregistre `portId` et `pedestalId` au passage. On montre ainsi la création à partir de rien.

Si le temps est très court, lancer les étapes 1 et 2 juste avant l'arrivée du jury et commencer à l'étape 3.

---

## Version A : Docker + Keycloak

Collection `XYZ-Smart-Marina-Gateway`. Tous les appels passent par `http://localhost:8080`.

| # | Requête Postman | Résultat attendu | Phrase au jury |
| --- | --- | --- | --- |
| 0 | **0 - Auth › Jeton ADMIN (admin.xyz)** | `200`, `access_token` enregistré | « Keycloak émet un jeton JWT signé. La Gateway en vérifie la signature, l'émetteur et l'expiration à chaque appel. » |
| 1 | **1 - Ports › POST Créer port** | `201`, `id: 1` | « Un seul point d'entrée, le 8080 : la Gateway route `/api/ports` vers port-service, qui n'est même pas accessible depuis l'extérieur de Docker. » |
| 2 | **2 - Bornes › POST Créer une borne** | `201`, borne `ACTIVE` sur le port 1 | « pedestal-service n'a pas accès à la base des ports. Il vérifie que le port existe par un appel REST : une base par service, aucune jointure entre services. » |
| 3 | **3 - Consommations › POST Enregistrer électricité** (12,5 kWh) | `201` | « Ici on a besoin d'une réponse pour décider (la borne est-elle active ?), donc REST. Une fois la consommation enregistrée, on publie un fait, donc Kafka. » |
| 4 | **4 - Notifications › GET Notifications non lues** | `200`, une notification `INFO` `CONSUMPTION_RECORDED` | « notification-service ne connaît pas consumption-service. Il a reçu l'événement métier `ConsumptionRecordedEvent`, publié seulement après le commit. » |
| 5 | **3 - Consommations › POST Eau au-dessus du seuil** (3,2 m³ > 2 m³), puis relancer **GET Notifications non lues** | `201`, puis une notification `INFO` et une alerte `WARNING` `HIGH_CONSUMPTION` | « Le même événement déclenche une alerte au-delà du seuil. Un futur service de facturation pourrait s'abonner au même topic sans modifier le producteur. » |
| 6 | **5 - Règles métier › DELETE Port qui a des bornes (409)** | `409` : « le port 1 possède encore 1 borne(s) » | « port-service demande à pedestal-service combien de bornes ce port possède. On refuse plutôt que de laisser des bornes orphelines. » |
| 7 | **0 - Auth › Jeton PORT_MANAGER du port 1**, puis **5 - Règles métier › POST Borne sur un autre port en PORT_MANAGER (403, RG-09)** | `403` | « La Gateway contrôle le rôle, mais la règle "un gestionnaire n'agit que sur son port" (RG-09) est une règle métier : c'est pedestal-service qui l'applique. » |
| 8 | Dans un terminal : `docker compose -f docker/docker-compose.yml stop pedestal-service`. Reprendre le **Jeton ADMIN**, relancer **DELETE Port qui a des bornes**. | `503` (le test Postman, qui attend 409, apparaît en rouge : c'est normal) | « Une dépendance arrêtée donne un 503 propre en moins de 3 secondes grâce au timeout, pas un 500 ni un blocage. Et on ne supprime pas à l'aveugle. » |
| 9 | `docker compose -f docker/docker-compose.yml start pedestal-service`, puis **6 - Observabilité › GET Santé de la Gateway** | `200`, `UP` | « Actuator expose l'état de chaque service. Docker relance le service, et la plateforme est de nouveau complète. » |

Ajouts possibles s'il reste du temps :

- **GET Sans jeton (401)** : la Gateway refuse un appel sans jeton.
- **En-têtes d'identité falsifiés** : un client qui envoie `X-User-Roles: ADMIN` n'obtient rien de plus, car la Gateway supprime ces en-têtes.
- **DELETE Borne avec consommations (409, RG-08)**.

---

## Version B : plan B local (`SECURITY_ENABLED=false`)

Même collection `XYZ-Smart-Marina-Gateway`, sans le dossier **0 - Auth**.

Phrase d'introduction : « Pour la démonstration locale, la Gateway tourne sans Keycloak. Le routage, les services, Kafka et les règles métier sont identiques ; seule l'authentification est désactivée. »

| # | Requête Postman | Résultat attendu | Phrase au jury |
| --- | --- | --- | --- |
| 1 | **1 - Ports › POST Créer port** | `201` | « Un seul point d'entrée, le 8080 : la Gateway route `/api/ports` vers port-service. Les clients ne connaissent qu'une adresse. » |
| 2 | **2 - Bornes › POST Créer une borne** | `201`, borne `ACTIVE` | « pedestal-service n'a pas accès à la base des ports. Il vérifie le port par un appel REST : une base par service. » |
| 3 | **3 - Consommations › POST Enregistrer électricité** | `201` | « REST quand il faut une réponse pour décider (la borne est-elle active ?), Kafka pour annoncer un fait accompli. » |
| 4 | **4 - Notifications › GET Notifications non lues** | `200`, une notification `INFO` | « notification-service a reçu l'événement métier `ConsumptionRecordedEvent` par Kafka, sans aucun appel direct entre les deux services. » |
| 5 | **3 - Consommations › POST Eau au-dessus du seuil**, puis relancer **GET Notifications non lues** | `201`, puis une alerte `WARNING` | « Le même événement déclenche une alerte au-delà de 2 m³. D'autres abonnés pourraient s'ajouter sans modifier le producteur. » |
| 6 | **5 - Règles métier › DELETE Port qui a des bornes (409)** | `409` | « port-service demande à pedestal-service si le port a encore des bornes : on refuse plutôt que de créer des bornes orphelines. » |
| 7 | Collection `XYZ-Smart-Marina` › **POST Borne sur le port d'un autre gestionnaire (403, RG-09) - plan B, appel direct** (sur `:8082`, avec `X-User-Roles: PORT_MANAGER` et `X-User-Port-Id: 1`) | `403` | « Sans Keycloak, je simule les en-têtes que la Gateway transmettrait pour le gestionnaire du port 1. pedestal-service applique RG-09 et refuse le port 2. » |
| 8 | Arrêter pedestal-service (`Ctrl+C` dans son terminal), relancer **DELETE Port qui a des bornes** | `503` (test Postman en rouge : normal) | « Service arrêté : 503 immédiat et propre, pas de 500. Le port n'est pas supprimé à l'aveugle. » |
| 9 | Relancer pedestal-service (`.\mvnw.cmd spring-boot:run`), puis **6 - Observabilité › GET Santé de la Gateway** | `200`, `UP` | « Actuator expose l'état de chaque service ; dans Docker, c'est ce que surveillent les sondes de santé. » |

À l'étape 7, être honnête si le jury relève qu'on peut forger ces en-têtes : c'est justement pourquoi, en version A, les services ne sont joignables que par la Gateway, qui supprime les en-têtes envoyés par le client (voir [SOUTENANCE.md](SOUTENANCE.md#limites-connues)).

---

## Minutage indicatif

| Temps | Étapes |
| --- | --- |
| 0:00 - 1:00 | Architecture en une phrase (schéma du README), étapes 0 à 2 |
| 1:00 - 2:30 | Étapes 3 à 5 : REST, puis Kafka et l'alerte |
| 2:30 - 4:00 | Étapes 6 à 8 : 409, 403 et 503 |
| 4:00 - 5:00 | Étape 9, puis conclusion : « une base par service, la Gateway comme seule entrée, REST pour décider, Kafka pour informer, la sécurité à deux niveaux » |

## Si quelque chose casse pendant la démonstration

| Symptôme | Cause probable | Réaction |
| --- | --- | --- |
| `401` partout (version A) | Jeton expiré (durée de vie de 30 minutes dans le realm) | Relancer **Jeton ADMIN** |
| `403` à l'étape 8 ou 9 (version A) | Le jeton manager de l'étape 7 est encore actif | Relancer **Jeton ADMIN** |
| `400` « port introuvable » à l'étape 2 | `portId` ne vaut pas l'identifiant créé | Relancer l'étape 1 : elle met à jour `portId` |
| Aucune notification à l'étape 4 | Kafka encore en démarrage, ou délai de traitement | Attendre 2 à 3 secondes et relancer le GET |
| Keycloak ne démarre pas ou les jetons sont refusés | Problème non testé de la version A | Passer à la version B (Gateway avec `SECURITY_ENABLED=false`) |
