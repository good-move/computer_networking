from threading import RLock


class IdGenerator:

    def __init__(self):
        self.__id = -1
        self.__lock = RLock()

    def get_next_id(self):
        self.__id += 1
        return self.__id
