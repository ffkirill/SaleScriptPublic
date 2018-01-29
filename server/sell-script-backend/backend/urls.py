"""billing URL Configuration

The `urlpatterns` list routes URLs to views. For more information please see:
    https://docs.djangoproject.com/en/1.9/topics/http/urls/
Examples:
Function views
    1. Add an import:  from my_app import views
    2. Add a URL to urlpatterns:  url(r'^$', views.home, name='home')
Class-based views
    1. Add an import:  from other_app.views import Home
    2. Add a URL to urlpatterns:  url(r'^$', Home.as_view(), name='home')
Including another URLconf
    1. Add an import:  from blog import urls as blog_urls
    2. Import the include() function: from django.conf.urls import url, include
    3. Add a URL to urlpatterns:  url(r'^blog/', include(blog_urls))
"""
from django.conf.urls import url, include
from django.contrib import admin
from django.contrib.auth.views import password_reset_confirm, password_change
from django.conf.urls.i18n import i18n_patterns

from django.contrib.sitemaps.views import sitemap
from django.core.urlresolvers import reverse
from django.contrib.sitemaps import Sitemap
from django.views.generic import RedirectView, TemplateView
from django.contrib.staticfiles.templatetags.staticfiles import static

from clients.views import do_login
from clients.forms import SaleScriptPasswordChangeForm
from common.views import MultilingualTemplateView, current_user


class ViewSitemap(Sitemap):
    """Reverse 'static' views for XML sitemap."""

    def items(self):
        # Return list of url names for views to include in sitemap
        return ['main', 'faq']

    def location(self, item):
        return reverse(item)

    i18n = True


sitemaps = {'views': ViewSitemap}


urlpatterns = [
    #SEO
    url(r'^robots\.txt$', TemplateView.as_view(
        template_name='robots.txt'), name='robots'),

    url(r'^sitemap\.xml$', TemplateView.as_view(
        template_name='sitemap.xml'), name='sitemap'),
    #

    url(r'^admin/', admin.site.urls),
    url(r'^api/current_user$', current_user),
    url(r'^api/', include('scripts.api_urls')),
    url('', include('social.apps.django_app.urls', namespace='social'))
]

urlpatterns += i18n_patterns(
    url(r'^client/', include('clients.urls')),

    #Redefine password reset confirm
    url(r'^reset/(?P<uidb64>[0-9A-Za-z_\-]+)/(?P<token>[0-9A-Za-z]{1,13}-[0-9A-Za-z]{1,20})/$',
        do_login(password_reset_confirm), name='password_reset_confirm',
        kwargs={'post_reset_redirect': 'clients:dashboard'}),  #

    #Redefine password change
    url(r'^password_change/$', password_change, name='password_change',
        kwargs={'password_change_form': SaleScriptPasswordChangeForm}),

    url(r'^', include('django.contrib.auth.urls')),

    url(r'^scripts/', include('scripts.urls')),

    url(r'^$', MultilingualTemplateView.as_view(
        template_name='main.html',
        multilang_template='main.{LANGUAGE_CODE}.html'), name="main"),

    url(r'^faq$', MultilingualTemplateView.as_view(
        template_name='faq.html',
        multilang_template='faq.{LANGUAGE_CODE}.html'), name="faq"),

    url(r'^termsofuse$', RedirectView.as_view(
        url=static('salescript_toe_ru.pdf')), name='toe'),

    url(r'^confidential$', RedirectView.as_view(
        url=static('salescript_confidential_ru.pdf')), name='confidential'),

)
