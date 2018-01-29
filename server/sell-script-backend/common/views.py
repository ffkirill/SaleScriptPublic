from django.views.generic import TemplateView

from rest_framework.decorators import api_view
from rest_framework.response import Response

from scripts.models import Script


class MultilingualTemplateView(TemplateView):
    multilang_template = None

    def get_template_names(self):
        templates = super().get_template_names()
        if (self.request.LANGUAGE_CODE is not None and
                self.multilang_template is not None):
            templates.insert(0, self.multilang_template.format(
                LANGUAGE_CODE=self.request.LANGUAGE_CODE.lower()))
        return templates


@api_view(['GET'])
def current_user(request):
    user = request.user
    return Response({
        'username': user.username,
        'id': user.pk,
        'email': user.email,
        'isSuperuser': user.is_superuser,
        'ownScripts':  {
            int(el['id']): el
            for el in Script.objects.allowed_to(user)
                                    .filter(has_perm_own=True)
                                    .values('id', 'title')
        }
    })
