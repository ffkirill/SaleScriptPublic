from django.conf.urls import url, include
from rest_framework_nested import routers
from .views import ScriptViewSet, ScriptPermissionViewSet

router = routers.DefaultRouter()
router.register(r'scripts', ScriptViewSet)

scripts_router = routers.NestedSimpleRouter(router, r'scripts', lookup='script')
scripts_router.register(r'permissions', ScriptPermissionViewSet,
                        base_name='script-permissions')


# Wire up our API using automatic URL routing.
# Additionally, we include login URLs for the browsable API.
urlpatterns = [
    url(r'^', include(router.urls)),
    url(r'^', include(scripts_router.urls)),
    url(r'^api-auth/', include('rest_framework.urls', namespace='rest_framework'))
]