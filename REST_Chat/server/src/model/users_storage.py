from .id_generator import IdGenerator
from .user import *


class UserStorage:

    def __int__(self):
        self.__id_user_map = dict()
        self.__username_user_map = dict()
        self.__id_generator = IdGenerator()

    def get_by_id(self, user_id: int) -> User:
        return self.__id_user_map.get(user_id, None)

    def get_by_username(self, username: str) -> User:
        return self.__username_user_map.get(username, None)

    def create_user(self, username: str, status: Status = Status.ONLINE):
        user_id = self.__id_generator.get_next_id()
        user = User(user_id, username, status)
        self.__id_user_map[user_id] = user
        self.__username_user_map[username] = user

    def update_status(self, status: Status, user_id=None, username=None):
        if user_id is None and username is None:
            raise ValueError('Cannot locate user: either user_id or username must be specified')

        if user_id is not None:
            self.update_status_with_key(self.__id_user_map, 'id', user_id, status)

        if username is not None:
            self.update_status_with_key(self.__id_user_map, 'username', username, status)

    def update_status_with_key(self, storage: dict, key_name: str, key_value, status: Status):
        user = storage.get(key_value)
        if user is None:
            raise ReferenceError('User with %s %d doesn\'t exist'.format(key_name, key_value))

        user.set_status(status)

    def get_online_users(self) -> list:
        online_users = list()
        for user_id, user in self.__id_user_map.items():
            if user.get_status() == Status.ONLINE or user.get_status() == Status.SLEEPING:
                online_users.append(user)

        return online_users
