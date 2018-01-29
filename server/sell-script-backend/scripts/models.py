from django.db import models
from django.db.models import Case, When, BooleanField, Value
from django.db.models.signals import post_save
from django.dispatch import receiver
from django.conf import settings
from django.utils.translation import ugettext_lazy as _

from django.contrib.auth.models import Permission
from django.contrib.contenttypes.models import ContentType


class ScriptQuerySet(models.QuerySet):
    def allowed_to(self, user):
        def perm_lookup_expr(*perms):
            return Case(When(
                pk__in=ScriptPermission.objects.filter(
                    user=user,
                    permission__in=ScriptPermission.perms(*perms))
                                                   .values('script__id'),
                then=Value(True)),
                    default=Value(False),
                    output_field=BooleanField())
        return (self
            .filter(pk__in=ScriptPermission.objects
                .filter(user=user)
                .values("script_id"))
            .annotate(
                has_perm_own=perm_lookup_expr('script_own'),
                has_perm_execute=perm_lookup_expr('script_own', 'script_execute'),
                has_perm_change=perm_lookup_expr('script_own', 'script_change'),
                has_perm_view=perm_lookup_expr('script_own', 'script_change',
                                               'script_execute', 'script_view'))
            .filter(has_perm_view=True))


class Script(models.Model):
    """
    Scenario model
    """
    objects = ScriptQuerySet.as_manager()
    title = models.CharField(verbose_name=_('title'), max_length=250)
    text = models.TextField()
    author = models.ForeignKey(to=settings.AUTH_USER_MODEL,
                               verbose_name=_('author'),
                               related_name='own_scripts')
    description = models.TextField(verbose_name=_('description'),
                                   blank=True, null=False)
    created = models.DateTimeField(verbose_name=_('creation date'),
                                   auto_now_add=True, editable=False)

    def __str__(self):
        return '{0} - {1}'.format(self.title, self.author)

    class Meta:
        permissions = (
            ('script_own', 'Script owner (r,w,x)'),
            ('script_execute', 'Run script in viewer with stats'),
            ('script_change', 'Save script in editor'),
            ('script_view', 'Run script in viewer w/o stats')
        )
        ordering = ('-created',)


class ScriptPermission(models.Model):
    """
    Script permission
    """
    script = models.ForeignKey(to=Script, related_name='permissions',
                               null=False, blank=False)
    user = models.ForeignKey(to=settings.AUTH_USER_MODEL, null=False,
                             blank=False)
    permission = models.ForeignKey(
        to='auth.Permission',
        null=False,
        blank=False,
        limit_choices_to=lambda: ScriptPermission.get_permissions_filter())

    @classmethod
    def get_permissions_filter(cls):
        return {'codename__in': (p[0] for p in Script._meta.permissions),
                'content_type': ContentType.objects.get_for_model(Script)}

    @classmethod
    def perms(cls, *args):
        return Permission.objects.filter(
            content_type=ContentType.objects.get_for_model(Script),
            codename__in=args)

    @classmethod
    def view_perms(cls):
        return cls.perms('script_own', 'script_execute', 'script_change',
                         'script_view')

    @classmethod
    def user_has_update_perm(cls, user, obj):
        perms = cls.perms('script_own', 'script_change')
        return cls.objects.filter(user=user, script=obj,
                                  permission__in=perms).exists()

    @classmethod
    def user_has_delete_perm(cls, user, obj):
        perms = cls.perms('script_own')
        return cls.objects.filter(user=user, script=obj,
                                  permission__in=perms).exists()

    def __str__(self):
        return '{0} - {1} - {2}'.format(self.script.title, self.user,
                                        self.permission)

    class Meta:
        unique_together = ('script', 'user', 'permission')


@receiver(post_save, sender=Script)
def script_post_save(sender, instance: Script,
                     created: bool, raw: bool, using,
                     *args, **kwargs):
    if created and not raw:
        ScriptPermission.objects.using(using).create(
            script=instance,
            user=instance.author,
            permission=Permission.objects.get_by_natural_key(
                'script_own',
                'scripts',
                'script'))
