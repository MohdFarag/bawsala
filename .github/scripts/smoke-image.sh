#!/usr/bin/env bash
set -euo pipefail
trap 'docker rm -f bawsala-smoke-api bawsala-smoke-db >/dev/null 2>&1 || true; docker network rm bawsala-smoke >/dev/null 2>&1 || true' EXIT
docker network create bawsala-smoke
docker run -d --name bawsala-smoke-db --network bawsala-smoke -e POSTGRES_PASSWORD=ci -e POSTGRES_DB=planner postgres:16-alpine
for _ in {1..60}; do
  if docker exec bawsala-smoke-db pg_isready -U postgres; then break; fi
  sleep 1
done
docker run -d --name bawsala-smoke-api --network bawsala-smoke -p 8080:8080 -e DATABASE_URL=postgres://postgres:ci@bawsala-smoke-db:5432/planner "${IMAGE:?}"
for _ in {1..60}; do
  if curl -fsS http://127.0.0.1:8080/healthz; then exit 0; fi
  sleep 1
done
docker logs bawsala-smoke-api
exit 1
