from rest_framework import serializers
from django.contrib.auth.models import Permission
from django.contrib.auth import get_user_model
from django.contrib.contenttypes.models import ContentType
from django.utils.functional import lazy
from django.db import IntegrityError
from django.db.models.query import QuerySet
from rest_framework.exceptions import ValidationError

from clients.models import get_or_create_user_with_email
from clients.utils import send_invitation

from .models import Script, ScriptPermission

User = get_user_model()


class ScriptSerializer(serializers.ModelSerializer):
    hasPermChange = serializers.BooleanField(source='has_perm_change',
                                             read_only=True)
    hasPermOwn = serializers.BooleanField(source='has_perm_own',
                                          read_only=True)

    def get_fields(self):
        fields = super().get_fields()
        if self.context['view'].action == 'list':
            fields.pop('text')
            fields.pop('author')
        return fields

    def create(self, validated_data):
        validated_data.pop('author_id', None)
        validated_data['author'] = self.context['request'].user
        return super().create(validated_data)

    class Meta:
        model = Script
        fields = ('pk', 'title', 'description', 'text', 'author',
                  'hasPermChange', 'hasPermOwn')
        read_only_fields = ('pk', 'author')


class UserSerializer(serializers.ModelSerializer):
    class Meta:
        model = User
        fields = ('username', 'email')
        extra_kwargs = {'username': {'read_only': True}}

    def to_internal_value(self, data):
        email = super().to_internal_value(data)['email'].strip().lower()
        user, created = get_or_create_user_with_email(email)
        if created:
            send_invitation(user, self.context['request'])
        return user


class ScriptPermissionSerializer(serializers.ModelSerializer):
    permission = serializers.SlugRelatedField(
        slug_field='codename',
        queryset=lazy(lambda: Permission.objects.filter(
            content_type=ContentType.objects.get_for_model(Script)), QuerySet)()
    )
    user = UserSerializer()

    class Meta:
        model = ScriptPermission
        fields = ('pk', 'user', 'permission')
        extra_kwargs = {'permission': lazy(lambda: {'queryset':
            Permission.objects.filter(
               **ScriptPermission.get_permissions_filter())}, dict)()}

    def create(self, validated_data):
        validated_data['script'] = self.context['view'].script
        try:
            instance = super().create(validated_data)
        except IntegrityError as e:
            raise ValidationError({"message": str(e)})
        return instance
