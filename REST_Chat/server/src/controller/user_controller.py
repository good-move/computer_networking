import uuid
from enum import Enum

import re

from pyrest.decorators import RouteController, POST, GET
from pyrest.http import HttpRequest, HttpResponse, HttpJsonResponse, HttpJsonRequest, Headers, ContentType
from src.model.user import Status, User
from src.model.users_storage import UserStorage


def check_auth(func):

    def decorator(self, request: HttpRequest, *args, **kwargs):
        token_header = request.headers.get(Headers.auth)
        if token_header is None:
            return HttpResponse(401)
        if not UserController.is_auth_header_format_valid(token_header):
            return HttpResponse(400)

        token_value = UserController.parse_auth_header(token_header)
        if self.authorized_users.get(token_value) is None:
            return HttpResponse(403)

        return func(self, request, *args, **kwargs)

    return decorator


@RouteController
class UserController:

    def __init__(self):
        self.__users_db = UserStorage()
        self.authorized_users = dict()

    @POST('/login')
    def login(self, request: HttpJsonRequest) -> HttpResponse:
        username = request.get_json().get('username')
        if self.__users_db.get_by_username(username) is None:
            user = self.__users_db.create_user(username)
            user_token = str(uuid.uuid4())

            self.authorized_users[user_token] = username

            return HttpJsonResponse({
                **self.__serialize_user(user),
                'token': user_token
            })

        return HttpResponse(401).add_header('WWW-Authenticate', 'Token realm=\'Username is already in use\'')

    @GET('/logout')
    @check_auth
    def logout(self, request: HttpRequest) -> HttpResponse:
        if not self.__check_auth(request):
            pass
        print('GET: ' + request.path)
        return HttpResponse()

    @GET('/users')
    @check_auth
    def get_user_list(self, request: HttpRequest) -> HttpResponse:
        users = [self.__serialize_user(user) for user in self.__users_db.get_online_users()]
        return HttpJsonResponse({
            'users': users
        })

    @GET('/users/{id:int}')
    @check_auth
    def get_user(self, request: HttpRequest, user_id: int) -> HttpResponse:
        print('GET ' + request.path)

        user = self.__users_db.get_by_id(user_id)
        if user is None:
            return HttpResponse(404)

        return HttpJsonResponse(self.__serialize_user(user)).add_header(Headers.content_type, ContentType.json)

    def __serialize_user(self, user: User) -> dict:
        return {
            'id': user.get_id(),
            'username': user.get_username(),
            'status': repr(user.get_status())
        }

    @staticmethod
    def is_auth_header_format_valid(auth_header: str) -> bool:
        print(auth_header)
        return re.match(r"^Authorization: Token [-a-zA-Z0-9]+$", auth_header) is not None

    @staticmethod
    def parse_auth_header(auth_header: str):
        return auth_header.split(' ')[2]
