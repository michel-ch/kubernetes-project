# MyService - Kubernetes Minikube

Application web composee d'un frontend Nginx, d'un backend Spring Boot et d'une base de donnees PostgreSQL, deployee sur Kubernetes via Minikube.

## Architecture

```
Client --> Ingress Nginx
              |
        ------+------
        |            |
    frontend     backend --> postgres
    (Nginx)    (Spring Boot)  (PostgreSQL)
```

- **Frontend** : page HTML statique servie par Nginx (port 80)
- **Backend** : API REST Spring Boot (port 8080) avec JPA/Hibernate
- **PostgreSQL** : base de donnees persistante (port 5432, ClusterIP interne)
- **Ingress** : `/api` -> backend, `/` -> frontend

## Pre-requis

- [Docker](https://docs.docker.com/get-docker/)
- [Minikube](https://minikube.sigs.k8s.io/docs/start/)
- [kubectl](https://kubernetes.io/docs/tasks/tools/)

## Demarrage

### 1. Lancer Minikube

```bash
minikube start
```

### 2. Activer l'addon Ingress

```bash
minikube addons enable ingress
```

### 3. Construire les images Docker

Utiliser le daemon Docker de Minikube pour que les images soient disponibles dans le cluster :

```bash
eval $(minikube docker-env)
```

Sur Windows (PowerShell) :

```powershell
& minikube -p minikube docker-env --shell powershell | Invoke-Expression
```

Construire les images :

```bash
docker build -t sneakyrat/backend:v3 ./backend
docker build -t sneakyrat/frontend:v1 ./frontend
```

### 4. Deployer sur Kubernetes

Appliquer les manifests dans cet ordre :

```bash
# Base de donnees
kubectl apply -f postgres-secret.yml
kubectl apply -f postgres.yml

# Securite
kubectl apply -f backend-serviceaccount.yml
kubectl apply -f networkpolicy-default-deny.yml
kubectl apply -f networkpolicy-backend.yml
kubectl apply -f networkpolicy-postgres.yml

# Applications
kubectl apply -f backend.yml
kubectl apply -f frontend.yml
kubectl apply -f ingress.yml
```

### 5. Verifier le deploiement

```bash
kubectl get pods
kubectl get services
kubectl get ingress
```

Attendre que tous les pods soient en status `Running` :

```bash
kubectl wait --for=condition=ready pod --all --timeout=120s
```

### 6. Acceder a l'application

Recuperer l'IP de Minikube :

```bash
minikube ip
```

Ou lancer le tunnel pour acceder via l'Ingress :

```bash
minikube tunnel
```

Tester :

```bash
# Page frontend
curl http://<MINIKUBE_IP>/

# Lister les messages
curl http://<MINIKUBE_IP>/api

# Creer un message
curl -X POST http://<MINIKUBE_IP>/api \
  -H "Content-Type: application/json" \
  -d '{"content": "Hello DB!"}'
```

## API

| Methode | Endpoint | Description |
|---------|----------|-------------|
| GET | `/api` | Liste tous les messages |
| POST | `/api` | Cree un message (`{"content": "..."}`) |

## Structure des fichiers

```
.
├── backend/                  # Code source Spring Boot
│   ├── Dockerfile
│   ├── build.gradle
│   └── src/
├── frontend/                 # Code source frontend
│   ├── Dockerfile
│   └── index.html
├── postgres-secret.yml       # Credentials PostgreSQL (Secret)
├── postgres.yml              # PVC + Deployment + Service PostgreSQL
├── backend-serviceaccount.yml# ServiceAccount backend
├── backend.yml               # Deployment + Service backend
├── frontend.yml              # Deployment + Service frontend
├── ingress.yml               # Ingress Nginx (routing)
├── networkpolicy-default-deny.yml  # Deny all ingress par defaut
├── networkpolicy-backend.yml       # Autorise frontend/ingress -> backend
└── networkpolicy-postgres.yml      # Autorise backend -> postgres
```

## Securite

- **Secrets** : les credentials PostgreSQL sont stockes dans un Secret Kubernetes
- **NetworkPolicies** : deny-all par defaut, regles explicites pour chaque flux autorise
- **SecurityContext** : containers non-root, capabilities droppees
- **ServiceAccount** : compte dedie pour le backend sans montage automatique de token
- **Resources** : limites CPU/memoire definies sur chaque container
