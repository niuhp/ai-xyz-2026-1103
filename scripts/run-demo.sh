#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if [[ -x /usr/libexec/java_home ]]; then
  JAVA_HOME="$(/usr/libexec/java_home -v 25)"
  export JAVA_HOME
fi
: "${JAVA_HOME:?请将 JAVA_HOME 设置为 JDK 25}"
export PATH="$JAVA_HOME/bin:$PATH"
export DEMO_UPSTREAM_PORT="${DEMO_UPSTREAM_PORT:-18083}"
export SERVER_PORT="${SERVER_PORT:-8082}"
for demo_port in "$DEMO_UPSTREAM_PORT" "$SERVER_PORT"; do
  if command -v lsof >/dev/null && lsof -nP -iTCP:"$demo_port" -sTCP:LISTEN >/dev/null; then
    echo "端口 $demo_port 已被占用，请先停止对应程序或改端口。" >&2
    exit 1
  fi
done
mvn -q -DskipTests package
python3 -u scripts/mock-model.py --port "$DEMO_UPSTREAM_PORT" &
mock_pid=$!
app_pid=''
cleanup() {
  if [[ -n "$app_pid" ]]; then kill "$app_pid" 2>/dev/null || true; fi
  kill "$mock_pid" 2>/dev/null || true
}
trap cleanup EXIT
trap 'exit 130' INT TERM
"$JAVA_HOME/bin/java" -jar target/ai-xyz-2026-1103-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=demo --spring.config.import= \
  --spring.ai.openai.base-url="http://127.0.0.1:$DEMO_UPSTREAM_PORT/v1" \
  --spring.ai.openai.api-key=local-demo-placeholder --server.port="$SERVER_PORT" &
app_pid=$!
echo "本地演示页面：http://localhost:$SERVER_PORT/?demo=1（Ctrl+C 停止两个进程）"
wait "$app_pid"
