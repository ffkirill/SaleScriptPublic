import json

from django.core.files import File
from django.http import HttpResponse
from django.views.generic import View, FormView
from django.views.decorators.cache import never_cache
from django import forms
from django.utils.translation import ugettext_lazy as _
from django.contrib import messages
from django.contrib.auth.mixins import LoginRequiredMixin

from rest_framework import viewsets
from rest_framework.permissions import BasePermission, SAFE_METHODS
from rest_framework import status
from rest_framework.response import Response

from .models import Script, ScriptPermission
from .serializers import ScriptSerializer, ScriptPermissionSerializer


class ScriptPerms(BasePermission):
    def has_permission(self, request, view):
        return request.user and request.user.is_authenticated()

    def has_object_permission(self, request, view, obj):
        if request.method == 'PUT':
            return obj.has_perm_change
        if request.method == 'DELETE':
            return True
        return request.method in SAFE_METHODS


class ScriptViewSet(viewsets.ModelViewSet):
    """
    API endpoint that allows scripts to be viewed or edited.
    """
    queryset = Script.objects.all()
    serializer_class = ScriptSerializer
    permission_classes = [ScriptPerms]

    def get_queryset(self):
        return super().get_queryset().allowed_to(self.request.user)

    def destroy(self, request, *args, **kwargs):
        instance = self.get_object()
        ScriptPermission.objects.filter(
            script=instance, user=request.user).delete()
        return Response(status=status.HTTP_204_NO_CONTENT)


class ScriptPermissionPerms(BasePermission):
    def has_permission(self, request, view):
        if view.script is None:
            view.set_script_from_request()
        return request.user and view.script.has_perm_change

    def has_object_permission(self, request, view, obj: ScriptPermission):
        if request.method not in SAFE_METHODS:
            return obj.user != obj.script.author
        return True



class ScriptPermissionViewSet(viewsets.ModelViewSet):
    """
    API endpoint that allows concrete script's permissions to be viewed or edited.
    """
    queryset = ScriptPermission.objects.all()
    serializer_class = ScriptPermissionSerializer
    script = None
    permission_classes = [ScriptPermissionPerms]

    def set_script_from_request(self):
        self.script = (
            Script.objects.allowed_to(self.request.user)
                .prefetch_related('permissions')
                .get(pk=self.kwargs['script_pk'], has_perm_own=True))

    def get_queryset(self):
        self.set_script_from_request()
        return self.script.permissions.get_queryset().order_by("user__username")

    def get_serializer_context(self):
        if self.script is None:
            self.set_script_from_request()
        return super().get_serializer_context()


class ExportScriptsView(LoginRequiredMixin, View):
    @never_cache
    def get(self, request, *args, **kwargs):
        vals = {'scripts': list(Script.objects.allowed_to(self.request.user).values(
            'title', 'text', 'description'))}
        for obj in vals['scripts']:
            obj['text'] = json.loads(obj['text'])
        response = HttpResponse(json.dumps(vals, ensure_ascii=False, indent=2),
                                content_type='application/json')
        response['Content-Disposition'] = 'attachment; filename="scripts.json"'
        return response


class ImportForm(forms.Form):
    upload = forms.FileField(label=_("File for upload"))
    delete_existed = forms.BooleanField(
        label=_("Remove existed scripts before loading data"),
        required=False
    )


class ImportScriptsView(LoginRequiredMixin, FormView):
    template_name = 'scripts/migrate.html'
    form_class = ImportForm
    success_url = '/'

    def create_scripts(self, data):
        for script in data['scripts']:
            Script.objects.create(
                title=script['title'],
                text=json.dumps(script['text']),
                description=script['description'],
                author=self.request.user
            )

    def clear_data(self):
        Script.objects.filter(author=self.request.user).delete()
        ScriptPermission.objects.filter(user=self.request.user).delete()

    def form_valid(self, form):
        upfile = self.request.FILES['upload']
        if hasattr(upfile, 'temporary_file_path'):
            upfile = upfile.temporary_file_path()
        with File(upfile) as file:
            if form.cleaned_data['delete_existed']:
                self.clear_data()
            values = json.loads(file.read().decode('utf-8'))
            self.create_scripts(values)
        messages.add_message(self.request, messages.INFO, _('Your scripts has been imported'))
        return super().form_valid(form)
