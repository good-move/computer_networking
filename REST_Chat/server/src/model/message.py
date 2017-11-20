class Message:

    def __init__(self, message_id: int, content: str, author_id: int):
        self.__id = message_id
        self.__content = content
        self.__author_id = author_id

    def get_id(self):
        return self.__id

    def get_content(self):
        return self.__content

    def get_author_id(self):
        return self.__author_id

    def __hash__(self):
        return self.__id.__hash__()