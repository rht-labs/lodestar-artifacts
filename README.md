# LodeStar Artifacts

This project provides artifact data for LodeStar.

The API is document via swagger and is available at `/q/swagger-ui`

----

## Configuration

The following environment variables are available:

### Logging
| Name | Default | Description|
|------|---------|------------|
| LODESTAR_LOGGING | DEBUG | Logging to the base source package |
| LODESTAR_LOGGING | DEBUG | Minimum logging level for the base package |
| MONGODB_USER | mongouser | The database user |
| MONGODB_PASSWORD | mongopassword | The database password |
| DATABASE_SERVICE_NAME | localhost:27017 | The host and port of the database |
| MONGODB_DATABASE | artifacts | The database name |

| GITLAB_API_URL | https://acmegit.com | The url to Gitlab |
| ENGAGEMENT_API_URL | https://acmegit.com | The url to Gitlab |
| GITLAB_TOKEN | t | The Access Token for Gitlab |

| DEFAULT_BRANCH | master | Default branch to use if default not found for project |
| DEFAULT_COMMIT_MESSAGE | updated artifacts list | Default commit message used if diff fails |
| DEFAULT_PAGE_SIZE | 20 | Default number of artifacts that will be returned if pageSize not specified |

## Deployment

See the deployment [readme](./deployment) for information on deploying to a OpenShift environment

## Running the application locally

### MongoDB 

A MongoDB database is needed for development. To spin up a docker MongoDB container run the following

```
cd deployment
docker-compose up
```

### Local Dev

You can run your application in dev mode that enables live coding using:

```
export GITLAB_API_URL=https://gitlab.com/ 
export ENGAGEMENT_API_URL=http://git-api:8080
export GITLAB_TOKEN=token
mvn quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at http://localhost:8080/q/dev/.
