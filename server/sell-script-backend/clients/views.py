from django.core.urlresolvers import reverse_lazy
from django.views.generic import TemplateView, FormView, CreateView, UpdateView
from django.contrib.auth import authenticate, login
from django.contrib.auth.mixins import LoginRequiredMixin
from django.contrib.auth import get_user_model, login as auth_login
from django.http.response import HttpResponseRedirect
from django.shortcuts import resolve_url
from django.utils.encoding import force_text
from django.utils.http import urlsafe_base64_decode
from django.conf import settings

from scripts.models import Script
from .forms import UserCreationForm, UserProfileForm

User = get_user_model()


class ClientRegistrationView(CreateView):
    """
    New user registration.
    """
    form_class = UserCreationForm
    template_name = 'clients/register.html'

    def form_valid(self, form):
        result = super().form_valid(form)
        user = authenticate(username=form.cleaned_data['username'],
                            password=form.cleaned_data['password1'])
        login(self.request, user)
        return result


class DashboardView(LoginRequiredMixin, TemplateView):
    """
    Dashboard - main client's view.
    Shows service expiration date, tariffs, bills, etc.
    """
    template_name = 'clients/dashboard.html'

    def get_context_data(self, **kwargs):
        data = super().get_context_data(**kwargs)
        data['scripts'] = Script.objects.allowed_to(self.request.user)
        return data


class ProfileView(LoginRequiredMixin, UpdateView):
    template_name = 'clients/profile.html'
    model = get_user_model()
    form_class = UserProfileForm
    success_url = reverse_lazy('clients:dashboard')

    def get_object(self, queryset=None):
        return self.request.user


def do_login(view):
    def wrapped(request, **kwargs):
        response = view(request, **kwargs)
        response_after_login = HttpResponseRedirect(
            resolve_url('clients:dashboard'))
        if (isinstance(response, HttpResponseRedirect) and
                str(response.url) == str(response_after_login.url)):
            uid = force_text(urlsafe_base64_decode(kwargs['uidb64']))
            user = User._default_manager.get(pk=uid)
            user.backend = settings.AUTHENTICATION_BACKENDS[0]
            auth_login(request, user)
            return response_after_login
        return response
    return wrapped
