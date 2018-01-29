from django import template
from django.conf import settings
from social.backends.utils import load_backends

register = template.Library()


@register.inclusion_tag('social_login.html', takes_context=True)
def social_login(context):
    return {'available_backends': load_backends(settings.AUTHENTICATION_BACKENDS)}
