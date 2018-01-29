# Sell Script project #

This repository contains django part of the project.
## Docker commands ##

1. Select proper machine:
`eval $(docker-machine env salescript)`
2. Build containers:
`docker-compose build`
3. Update containers: `docker-compose up -d`
4. Run database migration scripts: `docker-compose run --rm web /usr/local/bin/python manage.py migrate`
5. Collect static assets: `docker-compose run --rm web /usr/local/bin/python manage.py collectstatic --noinput`
6. Make compressed static assets: `docker-compose run --rm web /usr/local/bin/python manage.py mtime_cache --clean; docker-compose run --rm web /usr/local/bin/python manage.py compress`
