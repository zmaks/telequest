#!/bin/sh
docker-compose -f docker-compose-$1.yml pull
if ! docker-compose -f docker-compose-$1.yml kill $1; then
    echo "nothing to kill yet"
fi
docker-compose -f docker-compose-$1.yml up -d