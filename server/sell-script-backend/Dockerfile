FROM python:3.5-onbuild
RUN apt-get update && apt-get -y install gettext \
     && python manage.py compilemessages
