from django.conf.urls import url
from django.core.urlresolvers import reverse_lazy

from .views import (DashboardView, ProfileView, ClientRegistrationView)

app_name = 'clients'
urlpatterns = [
    url(r'^register', ClientRegistrationView.as_view(
            success_url=reverse_lazy('clients:dashboard')),
        name='register'),

    url(r'^dashboard', DashboardView.as_view(),
        name='dashboard'),

    url(r'^profile', ProfileView.as_view(),
        name='profile'),
]

