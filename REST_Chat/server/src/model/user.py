import uuid
from enum import Enum


class Status(Enum):
    OFFLINE='offline'
    ONLINE='online'
    SLEEPING='sleeping'


class User:

    def __init__(self, user_id: int, username: str, status: Status):
        self.__id = user_id
        self.__username = username
        self.__status = status

    def get_id(self):
        return self.__id

    def get_username(self):
        return self.__username

    def get_status(self):
        return self.__status

    def set_status(self, status: Status):
        self.__status = status

    def __hash__(self):
        return self.__username.__hash__()