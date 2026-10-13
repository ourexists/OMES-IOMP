#!/usr/bin/env bash
# OMES Docker — usage: ./docker/docker.sh <build|up|down> [infra]
set -euo pipefail
DEPLOY_ROOT="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$DEPLOY_ROOT/.." && pwd)"
cd "$ROOT"
WEB_ROOT="$ROOT/omes-iomp-web-admin"

[[ -f .env ]] || { cp "$DEPLOY_ROOT/.env.example" .env; echo "Created .env from .env.example"; }

compose() { docker compose --env-file "$ROOT/.env" -f "$DEPLOY_ROOT/docker-compose.yml" "$@"; }

cmd="${1:-}"
shift || true

case "$cmd" in
  build)
    [[ -f config/admin.properties && -f config/sas.properties && -f config/era-token.yml ]] || {
      echo "Missing config/admin.properties, config/sas.properties or config/era-token.yml (copy from *.example if needed)"
      exit 1
    }
    if [[ "${SKIP_MVN:-0}" != "1" ]]; then
      echo "==> mvn package"
      mvn -pl omes-iomp-runner-admin,omes-iomp-runner-sas -am clean package -DskipTests
    fi
    for module in omes-iomp-runner-admin omes-iomp-runner-sas; do
      jars=("$module"/target/"$module"-*.jar)
      [[ ${#jars[@]} -eq 1 && -f "${jars[0]}" ]] || { echo "Expected one executable JAR in $module/target; run a clean build"; exit 1; }
    done

    if [[ "${SKIP_WEB:-0}" != "1" ]]; then
      [[ -d "$WEB_ROOT" ]] || { echo "Missing $WEB_ROOT"; exit 1; }
      echo "==> vite build → dist"
      ( cd "$WEB_ROOT"
        [[ -d node_modules ]] || npm ci --no-audit --no-fund
        VITE_SAS_BASE_URL= npx vite build
      )
    fi
    [[ -f "$WEB_ROOT/dist/index.html" ]] || { echo "Missing frontend dist"; exit 1; }

    echo "==> docker compose build"
    compose --profile app build
    echo "OK → ./docker/docker.sh up"
    ;;
  up)
    target="${1:-all}"
    [[ "$target" == "infra" || "$target" == "all" ]] || { echo "Unknown target: $target"; exit 1; }
    if [[ "$target" == "infra" ]]; then
      compose up -d postgres redis rabbitmq
    else
      compose --profile app up -d
    fi
    compose ps
    echo "Web :8080 | SAS :9400 | Admin :10010 | RabbitMQ mgmt :15672"
    ;;
  down)
    compose --profile app down
    echo "Stopped."
    ;;
  *)
    echo "Usage: $0 <build|up|down> [infra]"
    echo "  build   SKIP_MVN=1 / SKIP_WEB=1 to skip steps"
    exit 1
    ;;
esac
