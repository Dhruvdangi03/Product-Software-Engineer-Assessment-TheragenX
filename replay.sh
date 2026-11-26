#!/usr/bin/env bash
# Rebuilds out/ from fixtures/, documents/ and the catalog. Needs no API key and no network.
#
#   ./replay.sh                      uses catalog/field_catalog.json
#   ./replay.sh path/to/catalog.json replays the same recordings against another catalog;
#                                    values the new catalog rejects, and fields the recordings
#                                    do not cover, come out unknown with a note saying why
set -euo pipefail

# Resolve the catalog argument against the caller's folder before moving to the repository root.
if [[ $# -ge 1 ]]; then
  CATALOG="$(realpath "$1")"
fi
cd "$(dirname "$0")"
CATALOG="${CATALOG:-catalog/field_catalog.json}"

./mvnw -q -DskipTests package
java -jar target/case-intake.jar --pipeline \
  --intake.llm.mode=replay \
  --intake.catalog-path="$CATALOG" \
  --logging.level.root=WARN --logging.level.com.theragenx=INFO
