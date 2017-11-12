from concurrent.futures import ThreadPoolExecutor
from hashlib import md5
import collections
import socket
import uuid

import time

from server.tcp_json_stream import TcpJsonStream
from server.message import *


class Range:
    def __init__(self, start, end):
        self.start = start
        self.end = end


class QueueEntry:
    def __init__(self, client_uuid: str, send_time):
        self.resolved = True
        self.uuid = client_uuid
        self.send_time = send_time


class ConcurrentServer:

    TIMEOUT = 3
    THREADS_COUNT = 5
    MAX_ANSWER_LENGTH = 30

    def __init__(self, target_hash: str, host: str, port: int):
        self.thread_pool = ThreadPoolExecutor(ConcurrentServer.THREADS_COUNT)
        self.hash = target_hash

        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        sock.bind((host, port))
        sock.listen(5)

        print("Listening on " + str(sock.getsockname()))

        self.socket = sock
        self.tcp_json_stream = TcpJsonStream()

        self.is_running = True
        self.answer_found = False
        self.registered_clients = set()

        """
        map: uuid --> (start, end, queue_entry),
        maps UUIDs of clients, which are currently
        trying to crack the hash, to their range start and end
        """
        self.ranges_for_client = dict()
        
        """
        queue of (uuid: str, send_time: timestamp, active: bool) objects
        to track clients, which take too much time 
        trying to crack the hash
        """
        self.sent_responses = collections.deque()

    def start(self):
        print("Server: Starting accepting connections...")
        while self.is_running:
            client_socket, client_address = self.socket.accept()
            print("Client connected: " + str(client_address))
            self.thread_pool.submit(self.__handle_request, client_socket)
        print("Server: Finishing work...")

    def close(self):
        # TODO: complete function
        self.is_running = False
        self.thread_pool.shutdown()
        self.socket.close()
        pass

    def __handle_request(self, client_socket: socket.socket):
        try:
            # read json
            request = self.tcp_json_stream.read_message(client_socket)
            request_code = int(request.get("code", -1))
            print("Request code: " + str(request_code))

            # choose a handler to handle current request
            handlers_map = {
                RequestCodes.REGISTER: self.__handle_registration_request,
                RequestCodes.GET_RANGE: self.__handle_get_range_request,
                RequestCodes.POST_ANSWER: self.__handle_post_answer_request
            }

            if request_code not in handlers_map.keys():
                self.__handle_unknown_request(client_socket, request)

            # pass request to the handler
            handlers_map.get(request_code, self.__handle_unknown_request)(client_socket, request)

        except ValueError as err:
            print(repr(err))
            error = ErrorResponseFactory.create(
                ErrorCodes.INVALID_MESSAGE_FORMAT,
                "Invalid message format"
            )
            self.tcp_json_stream.send_message(client_socket, error)
            client_socket.close()
        except Exception as err:
            print(repr(err))
            error = ErrorResponseFactory.create(
                ErrorCodes.INTERNAL_SERVER_ERROR,
                "Server encountered an unknown error"
            )
            self.tcp_json_stream.send_message(client_socket, error)
            client_socket.close()

    """
        Generates a random UUID for new client and sends it 
        with the hash the client has to break
        
        :param client_socket - client socket, which is used to respond to the client
        :param request (dict) - RegistrationRequest, format of which is specified in the
        protocol documentation
    """
    def __handle_registration_request(self, client_socket: socket.socket, request: dict):
        print("Client wants to register: " + str(client_socket.getsockname()))
        if not self.__is_answer_found():
            # generate uuid hex string
            client_uuid = uuid.uuid4().hex
            self.__register_client(client_uuid)
            response = SuccessResponseFactory.create_registration_response(client_uuid, self.hash)
            print("New client registered")
            self.tcp_json_stream.send_message(client_socket, response)
            # wait for GetRange request
            self.__handle_request(client_socket)
        else:
            response = ErrorResponseFactory.create(
                ErrorCodes.OUT_OF_RANGES,
                "Answer already found"
            )
            self.tcp_json_stream.send_message(client_socket, response)

    """
        Look for the next free range, which can be handed out to a client,
        and assign it to the current client

        :param client_socket - client socket, which is used to respond to the client
        :param request (dict) - GetRangeRequest, format of which is specified in the
        protocol documentation
    """
    def __handle_get_range_request(self, client_socket: socket.socket, request: dict):
        is_schema_valid = self.__validate_schema_and_reply_on_error(client_socket, request)

        if not is_schema_valid:
            client_socket.close()
            return

        if self.__is_answer_found():
            response = ErrorResponseFactory.create(
                ErrorCodes.OUT_OF_RANGES,
                "Answer already found"
            )
            self.tcp_json_stream.send_message(client_socket, response)
            client_socket.close()
            return

        reused_existing_range = False
        entry = self.__get_overdue_entry()
        while entry is not None:
            if entry.resolved:
                continue

            client_uuid = request.get(RequestSchema.uuid)
            free_range = self.ranges_for_client.get(entry.uuid)
            del self.ranges_for_client[entry.uuid]
            next_entry = QueueEntry(client_uuid, time.time())
            self.sent_responses.append(next_entry)
            self.ranges_for_client[client_uuid] = \
                (free_range.start, free_range.end, next_entry)
            self.tcp_json_stream.send_message(
                client_socket,
                SuccessResponseFactory.create_get_range_response(free_range.start, free_range.end)
            )
            reused_existing_range = True
            break

        if reused_existing_range:
            client_socket.close()
            return

        free_range = self.__generate_next_range()
        if free_range is None:
            response = ErrorResponseFactory.create(
                ErrorCodes.OUT_OF_RANGES,
                "Out of ranges"
            )
            self.tcp_json_stream.send_message(client_socket, response)
            client_socket.close()
            return

        client_uuid = request.get(RequestSchema.uuid)
        next_entry = QueueEntry(client_uuid, time.time())
        self.sent_responses.append(next_entry)
        self.ranges_for_client[client_uuid] = \
            (free_range.start, free_range.end, next_entry)
        self.tcp_json_stream.send_message(
            client_socket,
            SuccessResponseFactory.create_get_range_response(free_range.start, free_range.end)
        )

    """
        Checks if the so-called answer really is the answer, checks for
        string length limitations. If the answer is correct,
        sends Success response and starts terminating all active clients 

        :param client_socket - client socket, which is used to respond to the client
        :param request - PostAnswerRequest, format of which is specified in the
        protocol documentation
    """
    def __handle_post_answer_request(self, client_socket: socket.socket, request: dict):
        is_schema_valid = self.__validate_schema_and_reply_on_error(client_socket, request)

        try:
            if not is_schema_valid:
                return

            # check that answer is valid
            answer = request.get(RequestSchema.answer, None)

            if answer is None:
                self.tcp_json_stream.send_message(
                    client_socket,
                    ErrorResponseFactory.create(ErrorCodes.INVALID_MESSAGE_FORMAT, "Missing answer field")
                )
                return

            if self.__is_answer_correct(answer):
                response = SuccessResponseFactory.create_post_answer_response()
                self.tcp_json_stream.send_message(client_socket, response)
                self.__set_answer(answer)
            else:
                error = ErrorResponseFactory.create(ErrorCodes.WRONG_ANSWER, "Wrong answer: " + answer)
                self.tcp_json_stream.send_message(client_socket, error)
        except Exception as err:
            print(repr(err))
        finally:
            client_socket.close()

    """
        Sends the UnknownRequestCode error
    
        :param client_socket - client socket, which is used to respond to the client
        :param request - PostAnswerRequest, format of which is specified in the
        protocol documentation
    """
    def __handle_unknown_request(self, client_socket: socket.socket, request: dict):
        print("Received unknown request from: " + str(client_socket.getsockname()))

        error = ErrorResponseFactory.create(
            ErrorCodes.UNKNOWN_REQUEST_CODE,
            "Unknown request code: " + str(request.get("request_code"))
        )
        self.tcp_json_stream.send_message(client_socket, error)

    def __has_uuid(self, request: dict):
        # TODO: set mutex
        return RequestSchema.uuid in request

    def __register_client(self, uuid: str):
        # TODO: set mutex
        self.registered_clients.add(uuid)

    def __is_uuid_registered(self, uuid: str):
        # TODO: set mutex
        return uuid in self.registered_clients

    def __set_answer(self, answer: str):
        # TODO: set mutex
        self.answer = answer
        self.answer_found = True

    def __validate_schema_and_reply_on_error(self, client_socket: socket.socket, request: dict):
        # TODO: set mutex
        # check for uuid presence
        if not self.__has_uuid(request):
            self.tcp_json_stream.send_message(
                client_socket,
                ErrorResponseFactory.create(
                    ErrorCodes.MISSING_UUID,
                    "UUID is missing"
                )
            )
            return False

        # make sure the client is registered
        if not self.__is_uuid_registered(request[RequestSchema.uuid]):
            self.tcp_json_stream.send_message(
                client_socket,
                ErrorResponseFactory.create(
                    ErrorCodes.UNRECOGNIZED_UUID,
                    "UUID is not registered"
                )
            )
            return False

        return True

    def __is_answer_found(self):
        # TODO: set mutex
        return self.answer_found

    def __is_answer_correct(self, answer: str):
        return answer is not None and \
               self.hash == md5(answer).hexdigest() and \
               len(answer) <= ConcurrentServer.MAX_ANSWER_LENGTH

    def __get_overdue_entry(self) -> QueueEntry:
        # TODO: set mutex
        if len(self.sent_responses) == 0:
            return None

        entry = self.sent_responses.popleft()
        if time.time() - entry.send_time > ConcurrentServer.TIMEOUT:
            return entry
        else:
            self.sent_responses.appendleft(entry)
            return None

    def __generate_next_range(self) -> Range:
        return Range("", "AAA")
