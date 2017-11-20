from src.model.id_generator import IdGenerator
from src.model.message import Message


class MessageStorage:

    DEFAULT_HISTORY_SIZE = 100
    DEFAULT_HISTORY_OFFSET = -10

    def __init__(self):
        self.__message_history = list()
        self.__id_generator = IdGenerator()

    def add_to_history(self, content: str, author_id: int):
        self.__message_history.append(Message(self.__id_generator.get_next_id(), content, author_id))

    def get_history(self,
                    offset: int = DEFAULT_HISTORY_OFFSET,
                    history_size: int = DEFAULT_HISTORY_SIZE) -> list:
        end_index = offset+history_size
        if end_index >= 0 and offset < 0:
            end_index = len(self.__message_history)
        return self.__message_history[offset:end_index]
