import uuid
import re

import time

from pyrest.decorators import RouteController, POST, GET
from pyrest.http import HttpRequest, HttpResponse, HttpJsonResponse, HttpJsonRequest, Headers, ContentType, \
    ResponseMessages
from src.model.message import Message, MessageType
from src.model.message_storage import MessageStorage
from src.model.user import Status, User
from src.model.users_storage import UserStorage


def authorization(func):

    def decorator(self, request: HttpRequest, *args, **kwargs):
        token_header = request.headers.get(Headers.auth)
        if token_header is None:
            return HttpResponse(401, ResponseMessages.messages.get(401))
        if not AppController.is_auth_header_format_valid(token_header):
            return HttpResponse(400, ResponseMessages.messages.get(400))

        token_value = AppController.get_auth_header_value(token_header)
        if self.authorized_users.get(token_value) is None:
            return HttpResponse(403, ResponseMessages.messages.get(403))

        return func(self, request, *args, **kwargs)

    return decorator


def handle_idleness(func):

    def decorator(self, request: HttpRequest, *args, **kwargs):
        auth_token = AppController.get_auth_header_value(request.headers.get(Headers.auth))
        last_activity_time = self.last_activity_time.get(auth_token)
        if time.time() - last_activity_time > AppController.IDLENESS_TIMEOUT:
            self.purge_user_with_message(auth_token, Status.SLEEPING, 'went sleeping')
            return HttpResponse(403, ResponseMessages.messages.get(403))

        self.last_activity_time[auth_token] = time.time()
        return func(self, request, *args, **kwargs)

    return decorator


@RouteController
class AppController:

    DEFAULT_MESSAGE_LIST_SIZE = 10
    MAX_MESSAGE_LIST_SIZE = 10
    DEFAULT_OFFSET = -10
    IDLENESS_TIMEOUT = 300

    def __init__(self):
        self.__users_db = UserStorage()
        self.__messages_db = MessageStorage()
        self.authorized_users = dict()
        self.last_activity_time = dict()

    @POST('/login')
    def login(self, request: HttpJsonRequest) -> HttpResponse:
        # if content type is not a json actually
        if not re.match(ContentType.json, request.headers.get(Headers.content_type)):
            return HttpResponse(400, ResponseMessages.messages.get(400))

        username = request.get_json().get('username', None)

        # if request body doesn't contain required 'username' field
        if username is None:
            return HttpResponse(400, ResponseMessages.messages.get(400))

        if username not in self.authorized_users.values():
            user = self.__users_db.get_by_username(username)
            if user is None:
                user = self.__users_db.create_user(username)

            self.__users_db.update_status(Status.ONLINE, user.get_id())
            user_token = str(uuid.uuid4())
            self.authorized_users[user_token] = username
            self.last_activity_time[user_token] = time.time()

            self.__messages_db.add_to_history('joined chat', user.get_id(), MessageType.NOTIFICATION)

            return HttpJsonResponse({
                **self.__serialize_user(user),
                'token': user_token
            })

        return HttpResponse(401, ResponseMessages.messages.get(401))\
            .add_header('WWW-Authenticate', 'Token realm=\'Username is already in use\'')

    @GET('/logout')
    @authorization
    @handle_idleness
    def logout(self, request: HttpRequest) -> HttpResponse:
        auth_token = self.get_auth_header_value(request.headers.get(Headers.auth))
        self.purge_user_with_message(auth_token, Status.OFFLINE, 'left chat')

        return HttpJsonResponse({
            'message': 'bye!'
        })

    @GET('/users')
    @authorization
    @handle_idleness
    def get_user_list(self, request: HttpRequest) -> HttpResponse:
        users = [self.__serialize_user(user) for user in self.__users_db.get_online_users()]

        return HttpJsonResponse({
            'users': users
        })

    @GET('/users/{id:int}')
    @authorization
    @handle_idleness
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
    @handle_idleness
    def post_message(self, request: HttpJsonRequest) -> HttpResponse:
        # if content type is not a json actually
        if not re.match(ContentType.json, request.headers.get(Headers.content_type)):
            return HttpResponse(400, ResponseMessages.messages.get(400))

        message_content = request.get_json().get('message', None)
        # if required json field 'message' is missing
        if message_content is None:
            return HttpResponse(400, ResponseMessages.messages.get(400))

        auth_token = self.get_auth_header_value(request.headers.get(Headers.auth))
        # what if user logged out while we were processing this request???
        user = self.__users_db.get_by_username(self.authorized_users.get(auth_token))
        message = self.__messages_db.add_to_history(message_content, user.get_id())

        return HttpJsonResponse({
            'id': message.get_id(),
            'message': message_content
        })

    @GET('/messages')
    @authorization
    @handle_idleness
    def get_message_list(self, request: HttpRequest) -> HttpResponse:
        list_size = self.__init_int_value(
            request.query_params.get('count', None),
            AppController.DEFAULT_MESSAGE_LIST_SIZE
        )

        if list_size is None or list_size < 0:
            return HttpResponse(400, ResponseMessages.messages.get(400))

        offset = self.__init_int_value(
            request.query_params.get('offset', None),
            AppController.DEFAULT_OFFSET
        )

        if offset is None:
            return HttpResponse(400, ResponseMessages.messages.get(400))

        message_history = [
            self.__serialize_message(m) for m in self.__messages_db.get_history(offset, list_size)
        ]

        return HttpJsonResponse({
            'messages': message_history
        })

    def __serialize_message(self, message: Message) -> dict:
        return {
            'id': message.get_id(),
            'author_id': message.get_author_id(),
            'message': message.get_content(),
            'type': message.get_type().value
        }

    def __is_string_integer(self, string: str):
        return re.match(r"^-?\d+$", string) is not None

    def __init_int_value(self, string_value: str, default_value: int) -> int:
        if string_value is not None:
            if self.__is_string_integer(string_value):
                return int(string_value)
            else:
                return None

        return default_value

    def purge_user_with_message(self, auth_token: str, status: Status, message: str):
        user = self.__users_db.get_by_username(self.authorized_users.get(auth_token))
        self.__users_db.update_status(status, user.get_id())
        self.__messages_db.add_to_history(message, user.get_id(), MessageType.NOTIFICATION)
        del self.authorized_users[auth_token]
        del self.last_activity_time[auth_token]

    @staticmethod
    def is_auth_header_format_valid(auth_header: str) -> bool:
        return re.match(r"^Authorization: Token [-a-zA-Z0-9]+$", auth_header) is not None

    @staticmethod
    def get_auth_header_value(auth_header: str) -> str:
        return auth_header.split(' ')[2]
