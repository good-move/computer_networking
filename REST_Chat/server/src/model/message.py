from enum import Enum


class MessageType(Enum):
    TEXT="text"
    NOTIFICATION="notification"


class Message:

    def __init__(self, message_id: int, content: str, author_id: int, type: MessageType):
        self.__id = message_id
        self.__content = content
        self.__author_id = author_id
        self.__type = type

    def get_id(self):
        return self.__id

    def get_content(self):
        return self.__content

    def get_author_id(self):
        return self.__author_id

    def get_type(self):
        return self.__type

    def __hash__(self):
        return self.__id.__hash__()