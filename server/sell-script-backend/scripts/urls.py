from django.conf.urls import url
from django.views.generic import TemplateView
from .views import ExportScriptsView, ImportScriptsView

app_name = 'scripts'
urlpatterns = [
    url(r'^editor', TemplateView.as_view(template_name='scripts/editor.html'),
        name='editor'),
    url(r'^viewer', TemplateView.as_view(template_name='scripts/viewer.html'),
        name='viewer'),
    url(r'^preferences', TemplateView.as_view(template_name='scripts/preferences.html'),
        name='preferences'),
    url(r'^stats', TemplateView.as_view(template_name='scripts/stats-summary.html'),
        name='stats-all'),
    url(r'^download', ExportScriptsView.as_view(),
        name='download'),
    url(r'^migrate', ImportScriptsView.as_view(),
        name='migrate'),
]
