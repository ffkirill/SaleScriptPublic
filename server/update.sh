#!/usr/bin/env bash
docker-compose build
docker-compose up -d
docker-compose run --rm web /usr/local/bin/python manage.py migrate
docker-compose run --rm web /usr/local/bin/python manage.py collectstatic --noinput
docker-compose run --rm web /usr/local/bin/python manage.py mtime_cache --clean; docker-compose run --rm web /usr/local/bin/python manage.py compress
docker-compose restart web

