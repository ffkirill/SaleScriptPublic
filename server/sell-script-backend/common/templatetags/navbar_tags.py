from django import template

register = template.Library()


@register.inclusion_tag('navbar_user.html', takes_context=True)
def navbar_user(context):
    return {'user': context['user']}
