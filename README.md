# Smart Marina

Projet Atelier SOA - ISET Mahdia - Département TI (AU 2026/2027).
Plateforme microservices de gestion des ports de plaisance de XYZ : ports, bornes de service (eau / électricité), consommations et notifications.

## Architecture

```
 Frontend XYZ / Frontend Port / Frontend Plaisancier / Postman
                         │  JWT (Keycloak)
                         ▼
                 ┌───────────────┐
                 │  api-gateway  │ :8080   routage + validation JWT + CORS
                 └───────┬───────┘
     ┌───────────────┬───┴──────────────┬─────────────────────┐
     ▼               ▼                  ▼                     ▼
 port-service   pedestal-service   consumption-service   notification-service
   :8081    ◄──REST──► :8082 ◄──REST──►  :8083                 :8084
     │               │                  │   │ publie             ▲ consomme
  portdb         pedestaldb      consumptiondb  └──► Kafka ──────┘ notificationdb
                                         topic "consumption-recorded"
```

| Service | Port | Base (hôte) | Rôle |
| --- | --- | --- | --- |
| `api-gateway` | 8080 | — | Point d'entrée unique : routage, sécurité JWT, CORS, transmission de l'identité |
| `port-service` | 8081 | `portdb` (5433) | Référentiel des ports |
| `pedestal-service` | 8082 | `pedestaldb` (5434) | Bornes de service, liées à un port par `portId` |
| `consumption-service` | 8083 | `consumptiondb` (5435) | Mesure des consommations, producteur Kafka |
| `notification-service` | 8084 | `notificationdb` (5436) | Consommateur Kafka, notifications et alertes |
| Keycloak | 8180 | — | Serveur d'identité (realm `smart-marina`) |
| Kafka | 9092 | — | Bus d'événements (mode KRaft) |

Principes respectés :

- **Une base par service** : aucun service ne lit la base d'un autre.
- **Pas de relation JPA entre services** : `portId` et `pedestalId` sont de simples identifiants, vérifiés par appel REST.
- **DTO partout** : les entités ne sortent jamais des services. Le format d'erreur `ErrorResponse` est commun à tous les services.
- **Aucune logique métier dans la Gateway** : elle route, authentifie et applique des règles d'accès par rôle. Les règles métier (RG-08, RG-09) restent dans les services.
- **REST pour les questions, Kafka pour les faits** : une vérification qui a besoin d'une réponse immédiate (« cette borne existe-t-elle ? ») passe par REST. Un événement qui informe sans attendre de réponse (« une consommation a été enregistrée ») passe par Kafka.

## Démarrage

Prérequis : Java 21, Docker Desktop.

### Option 1 - toute la plateforme dans Docker (semaine 7)

```powershell
docker compose -f docker/docker-compose.yml --profile apps up -d --build
```

Seules la Gateway (`http://localhost:8080`) et Keycloak (`http://localhost:8180`) sont accessibles aux clients. Les microservices ne sont joignables qu'à l'intérieur du réseau Docker `marina-net`.

### Option 2 - infrastructure dans Docker, services depuis l'IDE

```powershell
docker compose -f docker/docker-compose.yml up -d      # 4 PostgreSQL, Kafka, Keycloak
cd port-service; .\mvnw.cmd spring-boot:run             # puis de même pour chaque service
```

Pour développer sans Keycloak, lancer la Gateway avec `SECURITY_ENABLED=false`.

### Tests

Chaque service se teste sans base ni Docker : les tests utilisent Mockito, MockMvc, H2 et un **Kafka embarqué**.

```powershell
cd pedestal-service; .\mvnw.cmd clean test
```

## Sécurité (semaine 8)

Keycloak émet des jetons JWT. La Gateway les valide (`spring-boot-starter-security-oauth2-resource-server`), puis transmet l'identité aux services avec les en-têtes `X-User-Id`, `X-User-Roles` et `X-User-Port-Id`. Les en-têtes envoyés par le client sont **toujours supprimés** : on ne peut pas se faire passer pour un ADMIN.

| Utilisateur | Mot de passe | Rôle | Port |
| --- | --- | --- | --- |
| `admin.xyz` | `admin123` | `ADMIN` | tous |
| `manager.port1` | `manager123` | `PORT_MANAGER` | 1 |
| `plaisancier` | `boater123` | `BOATER` | — |

Obtenir un jeton :

```powershell
curl -X POST http://localhost:8180/realms/smart-marina/protocol/openid-connect/token `
  -d grant_type=password -d client_id=smart-marina-app -d username=admin.xyz -d password=admin123
```

| Ressource | Lecture (GET) | Écriture |
| --- | --- | --- |
| `/api/ports` | authentifié | `ADMIN` |
| `/api/pedestals` | authentifié | `ADMIN`, `PORT_MANAGER` (son port uniquement, RG-09) |
| `/api/consumptions` | authentifié (un gestionnaire ne voit que son port) | POST : `ADMIN`, `PORT_MANAGER` ; DELETE : `ADMIN` |
| `/api/notifications` | authentifié (un gestionnaire ne voit que son port) | PATCH `/{id}/read` : authentifié |
| `/actuator/health` | public | — |

## API

La documentation Swagger UI de chaque service est disponible sur `http://localhost:<port>/swagger-ui.html`. Les collections Postman se trouvent dans [docs/](docs/). La collection [XYZ-Smart-Marina-Gateway](docs/XYZ-Smart-Marina-Gateway.postman_collection.json) déroule le scénario complet à travers la Gateway.

Le scénario de démonstration de 5 minutes (Docker + Keycloak, ou plan B local) se trouve dans [docs/DEMO.md](docs/DEMO.md). Les réponses aux questions de soutenance sont dans [docs/SOUTENANCE.md](docs/SOUTENANCE.md).

### port-service - `/api/ports`

`GET` (liste), `GET /{id}`, `POST`, `PUT /{id}`, `DELETE /{id}`. Corps : `{"name": "Marina Mahdia", "city": "Mahdia", "country": "Tunisie", "capacity": 250}`.

Codes d'erreur :
- `400` : validation ;
- `404` : port absent ;
- `409` : suppression d'un port qui possède encore des bornes ;
- `503` : `pedestal-service` indisponible au moment de la suppression.

### pedestal-service - `/api/pedestals`

| Méthode | Route | Description |
| --- | --- | --- |
| `POST` | `/api/pedestals` | Créer une borne (le port est vérifié par REST) |
| `GET` | `/api/pedestals?page=0&size=20&sort=id,asc` | Lister, paginé et trié (`size` ≤ 100) |
| `GET` | `/api/pedestals?portId=3&status=ACTIVE` | Filtrer par port et/ou statut |
| `GET` | `/api/pedestals/{id}` | Consulter une borne |
| `PUT` | `/api/pedestals/{id}` | Modifier une borne |
| `DELETE` | `/api/pedestals/{id}` | Supprimer (refusé si la borne a des consommations, RG-08) |

```json
{ "serialNumber": "XYZ-PED-001", "name": "Borne Ponton A1", "portId": 3, "pontoon": "A", "status": "ACTIVE" }
```

Statuts : `ACTIVE`, `INACTIVE`, `MAINTENANCE`. Codes d'erreur :

- `400` : validation, port inexistant ou champ de tri inconnu ;
- `403` : port d'un autre gestionnaire (RG-09) ;
- `404` : borne absente ;
- `409` : numéro de série déjà utilisé, ou borne avec consommations (RG-08) ;
- `503` : `port-service` ou `consumption-service` indisponible.

### consumption-service - `/api/consumptions`

| Méthode | Route | Description |
| --- | --- | --- |
| `POST` | `/api/consumptions` | Enregistrer une consommation, puis publier l'événement Kafka |
| `GET` | `/api/consumptions?pedestalId=&portId=&type=&sort=startedAt,desc` | Lister avec filtres |
| `GET` | `/api/consumptions/{id}` | Consulter |
| `GET` | `/api/consumptions/summary?pedestalId=5` | Nombre de consommations et totaux d'une borne |
| `DELETE` | `/api/consumptions/{id}` | Supprimer une saisie erronée (ADMIN) |

```json
{ "pedestalId": 5, "type": "ELECTRICITY", "quantity": 12.5, "boatName": "Sea Breeze",
  "startedAt": "2026-11-10T08:00:00Z", "endedAt": "2026-11-10T11:00:00Z" }
```

Types : `ELECTRICITY` (kWh) et `WATER` (m3). Règles :
- la quantité doit être strictement positive ;
- la période doit être dans le passé et se terminer après son début ;
- la borne doit exister (sinon `400`) et être `ACTIVE` (sinon `409`).

### notification-service - `/api/notifications`

`GET /api/notifications?portId=&unreadOnly=true` et `PATCH /api/notifications/{id}/read`.

## Flux événementiel (Kafka, semaine 6)

1. `consumption-service` enregistre la consommation, puis publie `ConsumptionRecordedEvent` sur le topic `consumption-recorded`.
   - L'événement n'est publié **qu'après le commit** (`@TransactionalEventListener`).
   - La clé du message est l'identifiant de la borne, ce qui garde l'ordre des événements par borne.
   - Le JSON est envoyé sans en-tête de type Java.
2. `notification-service` consomme l'événement (groupe `notification-service`) :
   - il crée une notification `INFO` ;
   - il crée aussi une alerte `WARNING` si la quantité dépasse un seuil (50 kWh ou 2 m3, configurables).
3. Robustesse :
   - le traitement est **idempotent** : la contrainte unique (événement, type) empêche les doublons quand Kafka redélivre un message ;
   - en cas d'échec : 3 nouvelles tentatives, puis envoi du message vers le topic `consumption-recorded-dlt`.

## Observabilité

Spring Boot Actuator est actif sur chaque service : `/actuator/health` (avec les sondes `liveness` et `readiness`), `/actuator/info` et `/actuator/metrics`.

## Règles de gestion

- **RG-08** : une borne qui a déjà des consommations ne peut pas être supprimée, car l'historique doit être conservé. `pedestal-service` interroge `consumption-service` avant de supprimer.
- **RG-09** : un gestionnaire de port ne gère que les bornes, les consommations et les notifications de son port. Il ne peut pas déplacer une borne vers un autre port.
- **Suppression d'un port** : un port qui possède encore des bornes ne peut pas être supprimé (`409`), sinon ces bornes pointeraient vers un port inexistant.
  - `port-service` interroge `pedestal-service` (`GET /api/pedestals?portId={id}&size=1`, lecture de `totalElements`).
  - L'appel a un délai de connexion de 2 s et de lecture de 3 s.
  - Si `pedestal-service` est indisponible, la réponse est `503` et la suppression est refusée par prudence.
