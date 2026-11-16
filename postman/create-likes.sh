#!/bin/bash

BASE_URL="http://localhost:8081"

PASSWORD="password123"

TOKENS=()

echo "================================"
echo "Creating 10 test users"
echo "================================"

for i in {1..10}
do
    USERNAME="perfuser$i"
    EMAIL="perfuser$i@gmail.com"

    echo "Creating $USERNAME..."

    RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/register" \
        -H "Content-Type: application/json" \
        -d "{
            \"username\": \"$USERNAME\",
            \"email\": \"$EMAIL\",
            \"password\": \"$PASSWORD\"
        }")

    echo "Register response: $RESPONSE"

    # Extract JWT
    TOKEN=$(echo "$RESPONSE" | python3 -c "
import sys, json
try:
    print(json.load(sys.stdin)['token'])
except:
    print('')
")

    # If registration failed because user already exists,
    # login instead.
    if [ -z "$TOKEN" ]; then

        echo "$USERNAME already exists. Logging in..."

        RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/login" \
            -H "Content-Type: application/json" \
            -d "{
                \"username\": \"$USERNAME\",
                \"password\": \"$PASSWORD\"
            }")

        TOKEN=$(echo "$RESPONSE" | python3 -c "
import sys, json
try:
    print(json.load(sys.stdin)['token'])
except:
    print('')
")
    fi

    if [ -z "$TOKEN" ]; then
        echo "ERROR: Could not get token for $USERNAME"
        exit 1
    fi

    TOKENS+=("$TOKEN")

    echo "$USERNAME token obtained."
    echo
done


echo "================================"
echo "Creating likes"
echo "================================"

for postId in {102..201}
do
    echo "Liking post $postId..."

    for token in "${TOKENS[@]}"
    do
        curl -s -X POST \
            "$BASE_URL/api/posts/$postId/likes/toggle" \
            -H "Authorization: Bearer $token" \
            > /dev/null
    done
done


echo
echo "================================"
echo "DONE"
echo "================================"
echo "100 posts × 10 users = 1000 likes"
