#!/bin/bash

set -e
set -o pipefail

DEPENDENCIES_LOCATION="$(mktemp -d)"

# Get the directory where THIS script lives
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Aim at and execute the dependencies installation
"$SCRIPT_DIR/dependencies.sh" build "$DEPENDENCIES_LOCATION"

#execute mvn clean install
mvn -f "$SCRIPT_DIR/pom.xml" clean install