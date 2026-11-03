#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

# macOS 默认选择 JDK 25；其他环境可显式设置 JAVA_HOME。
if [[ -z "${JAVA_HOME:-}" && -x /usr/libexec/java_home ]]; then
  JAVA_HOME="$(/usr/libexec/java_home -v 25)"
  export JAVA_HOME
fi
if [[ -z "${JAVA_HOME:-}" ]]; then
  echo "请将 JAVA_HOME 设置为 JDK 25 的安装目录" >&2
  exit 1
fi
export PATH="$JAVA_HOME/bin:$PATH"
mvn -q -DskipTests package
exec "$JAVA_HOME/bin/java" -jar target/ai-xyz-2026-1103-0.0.1-SNAPSHOT.jar
