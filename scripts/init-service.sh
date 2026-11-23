#!/usr/bin/env bash
# Turns a fresh copy of the template into a named service: renames the Java package
# (org.snippetsearcher.template -> org.snippetsearcher.<name>), the Gradle project, the Spring
# application, the database and the compose services. Run it once, right after creating the repo.
#
#   ./scripts/init-service.sh permissions
set -euo pipefail

name="${1:-}"
if [[ ! "$name" =~ ^[a-z][a-z0-9]*$ ]]; then
  echo "Usage: $0 <name>   (lowercase letters and digits, e.g. permissions, printscript)" >&2
  exit 1
fi

cd "$(dirname "$0")/.."

if [[ ! -d src/main/java/org/snippetsearcher/template ]]; then
  echo "This repository was already initialized: there is no org.snippetsearcher.template package." >&2
  exit 1
fi

for scope in main test; do
  mv "src/$scope/java/org/snippetsearcher/template" "src/$scope/java/org/snippetsearcher/$name"
done

# perl instead of sed -i: the same command works on Linux and macOS.
find src -name '*.java' -print0 |
  xargs -0 perl -pi -e "s/\borg\.snippetsearcher\.template\b/org.snippetsearcher.$name/g"

perl -pi -e "s/\btemplate\b/$name/g" \
  settings.gradle.kts \
  src/main/resources/application.yml \
  src/test/resources/application-test.yml \
  docker-compose.yml \
  Dockerfile

echo "Done: this is now the '$name' service. Next:"
echo "  1. ./gradlew build"
echo "  2. Edit the title and description at the top of README.md"
echo "  3. git add -A && git commit -m 'chore: initialize $name service from template'"
