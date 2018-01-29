SOCIAL_APPS = [
    'social.apps.django_app.default',
]

SOCIAL_TCP = [
    'social.apps.django_app.context_processors.backends',
]

SOCIAL_BACKENDS = [
    'social.backends.facebook.FacebookOAuth2',
    'social.backends.vk.VKOAuth2',
]

SOCIAL_AUTH_STRATEGY = 'social.strategies.django_strategy.DjangoStrategy'
SOCIAL_AUTH_STORAGE = 'social.apps.django_app.default.models.DjangoStorage'
SOCIAL_AUTH_GOOGLE_OAUTH_SCOPE = [
    'https://www.googleapis.com/auth/drive',
    'https://www.googleapis.com/auth/userinfo.profile'
]

SOCIAL_AUTH_VK_OAUTH2_KEY = '5324206'
SOCIAL_AUTH_VK_OAUTH2_SECRET = 'hu8w8hwaBYMWYwGmwgPr'

SOCIAL_AUTH_FACEBOOK_KEY = '198298767195527'
SOCIAL_AUTH_FACEBOOK_SECRET = '12cbf4e8af75bad167287fec262d32c0'

SOCIAL_AUTH_FACEBOOK_SCOPE = ['email']
SOCIAL_AUTH_VK_OAUTH2_SCOPE = ['email']

SOCIAL_AUTH_PIPELINE = (
    'social.pipeline.social_auth.social_details',
    'social.pipeline.social_auth.social_uid',
    'social.pipeline.social_auth.auth_allowed',
    'social.pipeline.social_auth.social_user',
    'social.pipeline.user.create_user',
    'social.pipeline.social_auth.associate_user',
    'social.pipeline.social_auth.load_extra_data',
    'social.pipeline.user.user_details',
)

USER_FIELDS = ['username', 'email', 'first_name', 'last_name']