# Development on OpenShift

## Getting Started With Helm

This directory contains a Helm chart which can be used to deploy a development version of this app for rapid testing.

Before you use it, you will need to download & install Helm 3.

If you are not familiar with Helm - how to configure it and run - you can start with this quickstart:

[https://helm.sh/docs/intro/quickstart](https://helm.sh/docs/intro/quickstart)

## Using This Chart

1. Clone the target repo:

```
git clone https://github.com/rht-labs/lodestar-artifacts
```

2. Change into to the `deployment` directory:

```
cd lodestar-artifacts/deployment
```

3. Deploy using the following Helm command:

```shell script
helm template . \
  --values values-dev.yaml \
  --set git.uri=<your fork> \
  --set git.ref=<your branch> \
  --set api.gitlab=<gitlabUrl> \
  --set tokens.gitlab=<gitlabToken> \
  --set db.mongodbServiceName=lodestar-artifacts-mongodb \
  --set db.mongodbUser=<your-mongodb-user> \
  --set db.mongodbPassword=<your-mongodb-password> \
  --set db.mongodbDatabase=artifacts \
  --set db.mongodbAdminPassword=<your-mongodb-admin-password> \
  --set config.gitlabGroupId=<gitlabGroupId> \
| oc apply -f -
```

It accepts the following variables

| Variable  | Use  |
|---|---|
| `git.uri` | The HTTPS reference to the repo (your fork!) to build  |
| `git.ref` | The branch name to build  |
| `api.gitlab` | The base URL of the GitLab instance to use |
| `db.mongodbServiceName` | MongoDB service name |
| `db.mongodbUser` | Application user for MongoDB |
| `db.mongodbPassword` | Application user password for MongoDB |
| `db.mongodbDatabase` | Application database name |
| `db.mongodbAdminPassword` | Admin password for MongoDB |
| `config.gitlabGroupId` | Gitlab group ID containing engagement projects |

This will spin up all of the usual resources that this service needs in production, plus a `BuildConfig` configured to build it from source from the Git repository specified. To trigger this build, use `oc start-build lodestar-artifacts`. To trigger a build from the source code on your machine, use `oc start-build lodestar-artifacts --from-dir=. -F`
