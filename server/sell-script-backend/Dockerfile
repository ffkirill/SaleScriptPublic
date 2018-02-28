FROM python:3.5-onbuild
RUN apt-get update && apt-get -y install gettext \
     && python manage.py compilemessages
CMD python manage.py migrate --noinput; \
	python manage.py collectstatic --noinput; \ 
	python manage.py mtime_cache --clean; \ 
	python manage.py compress; \ 
	/usr/local/bin/gunicorn backend.wsgi:application -w 2 -b :8000
