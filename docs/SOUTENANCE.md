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
