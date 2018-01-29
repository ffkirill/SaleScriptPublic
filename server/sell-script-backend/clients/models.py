from django.db import transaction, IntegrityError
from django.contrib.auth import get_user_model
from django.contrib.auth.models import AbstractUser


@transaction.atomic()
def get_or_create_user_with_email(email: str) -> (AbstractUser, bool):
    """
    Fetches or creates a user with the given email address.

    The User instance with given :param email will be returned if exists.
    Otherwise, returns instance of the new created user with the username
    of email (w/o domain parm) and an optional suffix to avoid a username
    collision.

    :returns Pair of User and Created flag
    """
    user_model = get_user_model()  # type: AbstractUser
    try:
        return user_model.objects.get(email=email), False
    except user_model.DoesNotExist:
        slug = email.split('@')[0]
        try:
            with transaction.atomic():
                user = user_model.objects.create(username=slug, email=email)
        except IntegrityError:
            suffix = 1
            while True:
                try:
                    with transaction.atomic():
                        user = user_model.objects.create(
                            username='{}{}'.format(slug, suffix), email=email)
                    break
                except IntegrityError:
                    if suffix < 9999:
                        suffix += 1
                    else:
                        raise
    return user, True
