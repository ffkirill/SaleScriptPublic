from django.test import TestCase
from django.contrib.auth.models import User
from clients.models import get_or_create_user_with_email


class UserCreationTest(TestCase):

    def testFetchExisted(self):
        u1 = User.objects.create(username='abc',
                                 email='abc@example.com')
        User.objects.create(username='abc22',
                            email='abc22@example.com')
        u2, created = get_or_create_user_with_email('abc@example.com')
        self.assertFalse(created)
        self.assertEqual(u1.pk, u2.pk)

    def testCreateWoCollision(self):
        u2, created = get_or_create_user_with_email('abc@example.com')
        self.assertTrue(created)
        self.assertEqual(u2.username, 'abc')

    def testCreateWithCollision(self):
        User.objects.create(username='abc',
                            email='aaa@example.com')

        for num in range(1, 6):
            User.objects.create(username='abc{}'.format(num),
                                email='abc{}@example.com'.format(num))

        u2, _ = get_or_create_user_with_email('abc@example.com')
        self.assertEqual(u2.username, 'abc6')

        User.objects.create(username='abc22',
                            email='aaa22@example.com')
        u2, _ = get_or_create_user_with_email('abc23@example.com')
        self.assertEqual(u2.username, 'abc23')
