# Questions de soutenance - réponses appuyées sur le projet

## 1. Pourquoi utiliser des microservices ?

Chaque domaine métier (ports, bornes, consommations, notifications) devient une application indépendante. Concrètement :

- chaque service est développé, testé, déployé et mis à l'échelle séparément ;
- par exemple, `consumption-service`, qui reçoit beaucoup de mesures, peut avoir plus d'instances que `port-service` ;
- une panne reste isolée : si `notification-service` tombe, on peut toujours enregistrer des consommations, et les événements attendent dans Kafka.

Le prix à payer : des appels réseau, une cohérence à terme et plus d'infrastructure (Gateway, Kafka, Docker).

## 2. Pourquoi une base de données par service ?

- Chaque service est **propriétaire de ses données** et peut changer son schéma sans casser les autres.
- Il n'y a pas de couplage caché : `pedestal-service` ne fait jamais de jointure SQL sur la table `ports`. Il appelle `GET /api/ports/{id}` par REST, via `PortClient`.
- C'est pour cela que `portId` est un simple `Long`, sans relation `@ManyToOne` entre services.
- Une base partagée créerait un monolithe distribué : les services seraient séparés dans le code mais liés par les données.

## 3. Quel est le rôle d'une API Gateway ?

C'est le **point d'entrée unique** (`:8080`) :

- elle route chaque préfixe vers le bon service ;
- elle valide le jeton JWT ;
- elle applique les règles d'accès par rôle et la politique CORS ;
- elle transmet l'identité de l'utilisateur aux services (en-têtes `X-User-*`, après avoir supprimé ceux que le client aurait envoyés).

Les clients ne connaissent qu'une adresse. Elle ne contient **aucune logique métier** : par exemple, la règle RG-09 est appliquée dans `pedestal-service`.

## 4. Quelle différence entre REST et Kafka ?

| | REST | Kafka |
| --- | --- | --- |
| Mode | Synchrone : on attend la réponse | Asynchrone : on publie, puis on continue |
| Couplage | L'appelant connaît le destinataire | Le producteur ignore qui consomme |
| Dans le projet | Vérifier qu'une borne existe et qu'elle est ACTIVE avant d'enregistrer | Annoncer « consommation enregistrée » à `notification-service` |
| Si le destinataire est absent | Erreur `503` immédiate | Le message attend dans le topic |

Règle suivie : REST quand on a besoin de la réponse pour décider, Kafka pour diffuser un fait accompli. Utiliser Kafka pour tout est une erreur : on ne peut pas valider une borne de façon asynchrone.

## 5. Pourquoi utiliser Docker ?

- On obtient le même environnement partout (poste de développement, correcteur, serveur).
- Une seule commande lance 4 PostgreSQL, Kafka, Keycloak et les 5 services.
- Chaque image est construite en deux étapes : compilation Maven, puis image légère avec seulement le JRE, exécutée par un utilisateur non-root.
- Le réseau Docker `marina-net` permet aux services de se trouver par leur nom (`http://port-service:8081`). En n'exposant que la Gateway, on empêche de la contourner.

## 6. Qu'est-ce qu'un événement métier ?

C'est un **fait passé et immuable** qui a du sens pour le métier, nommé au passé : `ConsumptionRecordedEvent`, « une consommation a été enregistrée ».

- Il ne donne pas d'ordre, il informe. Chaque abonné décide quoi en faire : `notification-service` crée une notification, et un futur service de facturation pourrait s'abonner sans modifier le producteur.
- Il est publié **après le commit**, pour ne jamais annoncer quelque chose qui n'a pas eu lieu.
- Il porte un `eventId`, qui permet un traitement idempotent.

## 7. Comment sécuriser une API ?

Dans le projet :

1. **Authentification** : Keycloak (OpenID Connect) émet un jeton JWT signé. La Gateway vérifie la signature (clés JWK), l'émetteur et l'expiration.
2. **Autorisation par rôle** dans la Gateway : `ADMIN`, `PORT_MANAGER`, `BOATER`, avec les codes `401` (pas de jeton) et `403` (rôle insuffisant).
3. **Autorisation métier** dans les services : RG-09, un gestionnaire n'agit que sur son port (`403`).
4. **Isolation réseau** : les services ne sont pas exposés hors de Docker, et les en-têtes d'identité falsifiés sont supprimés.
5. **Validation des entrées** avec Bean Validation, erreurs uniformes sans trace technique, sessions sans état et CSRF désactivé (API sans cookie).

En production, il faudrait ajouter HTTPS, des secrets hors du code (variables d'environnement ou coffre-fort), du *rate limiting*, et une authentification entre services (mTLS ou jetons *client credentials*).

## Limites connues

- **Confiance dans les en-têtes `X-User-*`.** Les microservices lisent l'identité dans les en-têtes posés par la Gateway et ne revérifient pas le jeton JWT. C'est sûr **uniquement dans Docker**, où les services ne publient aucun port vers l'hôte et ne sont joignables que par la Gateway.
  - Quand on lance les services depuis l'IDE, on peut appeler un service directement (`localhost:8082`) et forger ces en-têtes.
  - Amélioration possible : chaque service valide lui-même le JWT (resource server), ou mTLS entre services.
- **Docker et Keycloak écrits mais pas encore testés.** Les Dockerfiles, le `docker-compose.yml` et le realm Keycloak n'ont pas pu être exécutés : Docker n'était pas installé sur le poste de développement. Les 73 tests automatisés tournent sans Docker (Mockito, MockMvc, H2, Kafka embarqué) et couvrent le code Java, y compris le flux Kafka réel. Il reste à vérifier :
  - la construction des images ;
  - l'import du realm, en particulier la claim `port_id` ;
  - l'acceptation par la Gateway des jetons émis par Keycloak.
- **Plan B pour la démonstration.** Si Keycloak ne démarre pas ou si les jetons sont refusés, lancer la Gateway avec `SECURITY_ENABLED=false`.
  - Le routage, les services, Kafka et les règles métier restent démontrables.
  - Les règles d'accès par rôle et RG-09 ne s'appliquent plus, car aucune identité n'est transmise.
  - On peut alors montrer RG-09 en appelant directement un service avec les en-têtes `X-User-Roles: PORT_MANAGER` et `X-User-Port-Id: 1`.
- **Cohérence entre services.** Les vérifications croisées (port qui a des bornes, borne qui a des consommations) se font par REST au moment de la suppression. Ce n'est pas une transaction distribuée : une borne créée exactement pendant la suppression du port pourrait passer. C'est acceptable à cette échelle, et l'alternative serait un mécanisme de saga.
- **Dépendance circulaire REST.** `port-service` et `pedestal-service` s'appellent l'un l'autre, de même que `pedestal-service` et `consumption-service`. Si l'un est arrêté, certaines opérations de l'autre renvoient `503`, mais aucun service ne plante.
