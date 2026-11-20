#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if [[ -z "${JAVA_HOME:-}" && -x /usr/libexec/java_home ]]; then
  JAVA_HOME="$(/usr/libexec/java_home -v 25)"
  export JAVA_HOME
fi
if [[ -z "${JAVA_HOME:-}" ]]; then
  echo "请先将 JAVA_HOME 指向 JDK 25" >&2
  exit 1
fi
export PATH="$JAVA_HOME/bin:$PATH"
evaluation_split="${1:-tune}"
if [[ "$evaluation_split" != tune && "$evaluation_split" != regression ]]; then
  echo "用法：./scripts/evaluate.sh tune|regression [新输出目录] [基线 report.json]" >&2
  exit 1
fi
evaluation_output="${2:-.local/evaluation/${evaluation_split}-$(date +%Y%m%d-%H%M%S)}"
evaluation_baseline="${3:-}"
evaluation_revision="$(git rev-parse --short HEAD)"
if [[ -n "$(git status --porcelain)" ]]; then
  evaluation_revision+="-dirty"
fi
mvn -q -DskipTests package
# 独立进程，无监听端口；评测完成后由应用关闭上下文。
exec "$JAVA_HOME/bin/java" -jar target/ai-xyz-2026-1103-0.0.1-SNAPSHOT.jar \
  --spring.main.web-application-type=none \
  '--spring.datasource.url=jdbc:h2:mem:evaluation;DB_CLOSE_DELAY=-1' \
  --app.evaluation.enabled=true \
  "--app.evaluation.split=$evaluation_split" \
  "--app.evaluation.output=$evaluation_output" \
  "--app.evaluation.label=$evaluation_split-$evaluation_revision" \
  "--app.evaluation.revision=$evaluation_revision" \
  "--app.evaluation.baseline=$evaluation_baseline"
