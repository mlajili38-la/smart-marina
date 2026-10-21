# Smart Marina

Projet Atelier SOA - ISET Mahdia - Departement TI.
Architecture microservices : port-service, pedestal-service, consumption-service, notification-service, api-gateway.

## pedestal-service

Référentiel des bornes de service des ports de plaisance. Le service écoute sur le port **8082** et stocke ses données dans la base PostgreSQL `pedestaldb` sur `localhost:5434` (utilisateur `pedestal`).

Le champ `portId` est une référence distante vers `port-service` (port **8081**) : aucune relation ni base de données n'est partagée. Lors de la création ou de la modification, le port est vérifié via `GET /api/ports/{id}`. Le service des ports doit donc être disponible.

### Routes

| Méthode | Route | Description |
| --- | --- | --- |
| `POST` | `/api/pedestals` | Créer une borne |
| `GET` | `/api/pedestals?page=0&size=20&sort=id,asc` | Lister les bornes, paginées et triées |
| `GET` | `/api/pedestals?portId=3&status=ACTIVE` | Filtrer par port et/ou statut |
| `GET` | `/api/pedestals/{id}` | Consulter une borne |
| `PUT` | `/api/pedestals/{id}` | Modifier une borne |
| `DELETE` | `/api/pedestals/{id}` | Supprimer une borne |

Corps de création :

```json
{
  "serialNumber": "XYZ-PED-001",
  "name": "Borne Ponton A1",
  "portId": 3,
  "pontoon": "A",
  "status": "ACTIVE"
}
```

Les statuts autorisés sont `ACTIVE`, `INACTIVE` et `MAINTENANCE`. La validation des champs renvoie `400`, un numéro de série déjà utilisé renvoie `409`, une borne absente renvoie `404`, un port inexistant renvoie `400` et l'indisponibilité de `port-service` renvoie `503`. La suppression est directe pour cette version (RG-08, réservations et consommations, à intégrer avec les prochains services). La sécurité par port RG-09 est hors périmètre et sera traitée en semaine 8 avec JWT/Keycloak.

### Lancer et tester

Depuis `pedestal-service/`, démarrer le service avec :

```powershell
.\mvnw.cmd spring-boot:run
```

Exécuter les tests sans démarrer PostgreSQL ni `port-service` :

```powershell
.\mvnw.cmd test
```

La documentation Swagger UI est disponible sur [http://localhost:8082/swagger-ui.html](http://localhost:8082/swagger-ui.html).
