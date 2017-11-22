import uuid
import re

from pyrest.decorators import RouteController, POST, GET
from pyrest.http import HttpRequest, HttpResponse, HttpJsonResponse, HttpJsonRequest, Headers, ContentType
from src.model.message_storage import MessageStorage
from src.model.user import Status, User
from src.model.users_storage import UserStorage


def authorization(func):

    def decorator(self, request: HttpRequest, *args, **kwargs):
        token_header = request.headers.get(Headers.auth)
        if token_header is None:
            return HttpResponse(401, 'Unauthorized')
        if not AppController.is_auth_header_format_valid(token_header):
            return HttpResponse(400, 'Bad Request')

        token_value = AppController.get_auth_header_value(token_header)
        if self.authorized_users.get(token_value) is None:
            return HttpResponse(403, 'Forbidden')

        return func(self, request, *args, **kwargs)

    return decorator


@RouteController
class AppController:

    def __init__(self):
        self.__users_db = UserStorage()
        self.__messages_db = MessageStorage()
        self.authorized_users = dict()

    @POST('/login')
    def login(self, request: HttpJsonRequest) -> HttpResponse:
        username = request.get_json().get('username')
        if username not in self.authorized_users.values():
            user = self.__users_db.get_by_username(username)
            if user is None:
                user = self.__users_db.create_user(username)

            user_token = str(uuid.uuid4())
            self.authorized_users[user_token] = username

            return HttpJsonResponse({
                **self.__serialize_user(user),
                'token': user_token
            })

        return HttpResponse(401).add_header('WWW-Authenticate', 'Token realm=\'Username is already in use\'')

    @GET('/logout')
    @authorization
    def logout(self, request: HttpRequest) -> HttpResponse:
        auth_token = self.get_auth_header_value(request.headers.get(Headers.auth))
        self.__users_db.update_status(Status.OFFLINE, username=self.authorized_users.get(auth_token))
        del self.authorized_users[auth_token]

        return HttpJsonResponse({
            'message': 'bye!'
        })

    @GET('/users')
    @authorization
    def get_user_list(self, request: HttpRequest) -> HttpResponse:
        users = [self.__serialize_user(user) for user in self.__users_db.get_online_users()]

        return HttpJsonResponse({
            'users': users
        })

    @GET('/users/{id:int}')
    @authorization
    def get_user(self, request: HttpRequest, user_id: int) -> HttpResponse:
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

    @POST('/messages')
    @authorization
    def post_message(self, request: HttpRequest) -> HttpResponse:
        return HttpResponse()

    @GET('/messages')
    @authorization
    def get_message_list(self, request: HttpRequest) -> HttpResponse:
        return HttpResponse()

    @staticmethod
    def is_auth_header_format_valid(auth_header: str) -> bool:
        return re.match(r"^Authorization: Token [-a-zA-Z0-9]+$", auth_header) is not None

    @staticmethod
    def get_auth_header_value(auth_header: str) -> str:
        return auth_header.split(' ')[2]
