#!/usr/bin/env bash

#Utilisaton : ./smoke_test.sh [URL]
set -euo pipefail
BASE_URL="${1:-http://localhost}"
RETRIES=30 # 30 *5 s
FAILED=0

check(){
    local name="$1" url="$2" expect="$3"
    for _ in $(seq  1 "$RETRIES"); do 
        body=$(curl -fsS --max-time 5 "$url" 2>/dev/null || true)
        if [[ "$body" == *"$expect"* ]]; then 
            echo "OK   $name ($url)"
            return 0
        fi 
        sleep 5
    done
    echo "ECHEC   $name ($url) : '$expect' introuvable"
    FAILED=1
}

check "frontend" "$BASE_URL/"  "<app-root"
check "backend"  "$BASE_URL/actuator/health" '"status":"UP"'

exit "$FAILED"