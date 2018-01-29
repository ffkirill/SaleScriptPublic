from backend.settings import *
DEBUG = False
TEMPLATE_DEBUG = DEBUG
COMPRESS_OFFLINE = True
ALLOWED_HOSTS=['*']
DATABASES = {
    'default': {
        'ENGINE': 'django.db.backends.postgresql_psycopg2',
        'NAME': 'postgres',
        'USER': 'postgres',
        'PASSWORD': 'postgres',
        'HOST': 'postgres',
        'PORT': 5432
    }
}


DEFAULT_FROM_EMAIL = 'info@salescript.gift'

SECURE_PROXY_SSL_HEADER = ('HTTP_X_FORWARDED_PROTO', 'https')
# SESSION_COOKIE_SECURE = True
# CSRF_COOKIE_SECURE = True
# SECURE_HSTS_SECONDS = 3600

# EMAIL_BACKEND = 'django.core.mail.backends.smtp.EmailBackend'
# EMAIL_HOST = 'smtp.yandex.ru'
# EMAIL_PORT = 465
# EMAIL_HOST_USER = 'info@salescript.gift'
# EMAIL_HOST_PASSWORD = ''
# EMAIL_USE_SSL = True