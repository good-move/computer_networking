from concurrent.futures import ThreadPoolExecutor
from hashlib import md5
import collections
import threading
import socket
import uuid
import time

import sys

from server.utils import eprint
from server.permutation import PermutationGenerator
from server.tcp_json_stream import TcpJsonStream
from server.message import *


class Range:
    def __init__(self, start, end):
        self.start = start
        self.end = end


class QueueEntry:
    def __init__(self, client_uuid: str, send_time):
        self.resolved = False
        self.uuid = client_uuid
        self.send_time = send_time


def synchronized(method):
    def internal_method(self, *arg, **kwargs):
        with self.lock:
            return method(self, *arg, **kwargs)

    return internal_method


class ConcurrentServer:

    TIMEOUT = 3
    RANGE_SIZE = 1000
    THREADS_COUNT = 5
    MAX_ANSWER_LENGTH = 10
    DEFAULT_ALPHABET = 'ACGT'

    def __init__(self, target_hash: str, host: str, port: int):
        self.address = (host, port)
        self.thread_pool = ThreadPoolExecutor(ConcurrentServer.THREADS_COUNT)
        self.hash = target_hash
        self.perm_gen = PermutationGenerator(
            ConcurrentServer.MAX_ANSWER_LENGTH, ConcurrentServer.DEFAULT_ALPHABET)
        self.lock = threading.RLock()
        self.shutdown_thread = None

        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        sock.bind((host, port))
        sock.listen(5)

        print("Listening on " + str(sock.getsockname()))

        self.socket = sock
        self.tcp_json_stream = TcpJsonStream()

        self.is_running = True
        self.is_shutting = False
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
        self.sent_responses = collections.deque(maxlen=1000)

    def start(self):
        print("Server: Starting accepting connections...")
        while self.is_running:
            client_socket, client_address = self.socket.accept()
            if self.is_running and client_socket is not None:
                print("Client connected: " + str(client_address))
                self.thread_pool.submit(self.__handle_request, client_socket)
        print("Server: Finishing work...")

    def shutdown(self):
        self.__release_all_clients()
        self.__close()

    def __handle_request(self, client_socket: socket.socket):
        try:
            # read json
            request = self.tcp_json_stream.read_message(client_socket)
            request_code = int(request.get("code", -1))
            eprint("Request code: " + str(request_code))

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
            eprint(repr(err))
            error = ErrorResponseFactory.create(
                ErrorCodes.INVALID_MESSAGE_FORMAT,
                "Invalid message format"
            )
            self.tcp_json_stream.send_message(client_socket, error)
            client_socket.close()
        except Exception as err:
            eprint(repr(err))
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
        eprint("Client wants to register: " + str(client_socket.getsockname()))
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

        self.__resolve_request(request)

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
            eprint("looking for overdue entry")

            if entry.resolved:
                entry = self.__get_overdue_entry()
                continue

            client_uuid = request.get(RequestSchema.uuid)
            free_range = self.ranges_for_client.get(entry.uuid)
            del self.ranges_for_client[entry.uuid]
            next_entry = QueueEntry(client_uuid, time.time())
            self.sent_responses.append(next_entry)
            self.ranges_for_client[client_uuid] = \
                (free_range[0], free_range[1], next_entry)
            self.tcp_json_stream.send_message(
                client_socket,
                SuccessResponseFactory.create_get_range_response(free_range[0], free_range[1])
            )
            reused_existing_range = True
            eprint("reused range: " + str(free_range))
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
            self.__init_shutdown()
            self.tcp_json_stream.send_message(client_socket, response)
            client_socket.close()
            return

        eprint("generated new range: " + str(free_range))
        client_uuid = request.get(RequestSchema.uuid)
        next_entry = QueueEntry(client_uuid, time.time())
        self.sent_responses.append(next_entry)
        self.ranges_for_client[client_uuid] = \
            (free_range.start, free_range.end, next_entry)
        self.tcp_json_stream.send_message(
            client_socket,
            SuccessResponseFactory.create_get_range_response(free_range.start, free_range.end)
        )
        client_socket.close()

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

        self.__resolve_request(request)

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
                if not self.__is_answer_found():
                    self.__set_answer(answer)
                    self.__init_shutdown()
                response = SuccessResponseFactory.create_post_answer_response()
                self.tcp_json_stream.send_message(client_socket, response)
            else:
                error = ErrorResponseFactory.create(ErrorCodes.WRONG_ANSWER, "Wrong answer: " + answer)
                self.tcp_json_stream.send_message(client_socket, error)
        except Exception as err:
            eprint(repr(err))
        finally:
            client_socket.close()

    """
        Sends the UnknownRequestCode error
    
        :param client_socket - client socket, which is used to respond to the client
        :param request - PostAnswerRequest, format of which is specified in the
        protocol documentation
    """
    def __handle_unknown_request(self, client_socket: socket.socket, request: dict):
        eprint("Received unknown request from: " + str(client_socket.getsockname()))

        error = ErrorResponseFactory.create(
            ErrorCodes.UNKNOWN_REQUEST_CODE,
            "Unknown request code: " + str(request.get("request_code"))
        )
        self.tcp_json_stream.send_message(client_socket, error)

    @synchronized
    def __has_uuid(self, request: dict):
        return RequestSchema.uuid in request

    @synchronized
    def __register_client(self, uuid: str):
        self.registered_clients.add(uuid)

    @synchronized
    def __is_uuid_registered(self, uuid: str):
        return uuid in self.registered_clients

    @synchronized
    def __set_answer(self, answer: str):
        self.answer = answer
        self.answer_found = True

    @synchronized
    def __validate_schema_and_reply_on_error(self, client_socket: socket.socket, request: dict):
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

    @synchronized
    def __is_answer_found(self):
        return self.answer_found

    def __is_answer_correct(self, answer: str):
        return answer is not None and \
            self.hash == md5(answer.encode('utf-8')).hexdigest() and \
            len(answer) <= ConcurrentServer.MAX_ANSWER_LENGTH

    @synchronized
    def __get_overdue_entry(self) -> QueueEntry:
        if len(self.sent_responses) == 0:
            return None

        entry = self.sent_responses.popleft()

        if time.time() - entry.send_time > ConcurrentServer.TIMEOUT:
            return entry
        else:
            self.sent_responses.appendleft(entry)
            return None

    def __generate_next_range(self) -> Range:
        next_range = self.perm_gen.get_next_range(self.RANGE_SIZE)
        if len(next_range) == 0:
            return None
        else:
            return Range(next_range[0], next_range[1])

    @synchronized
    def __resolve_request(self, request: dict):
        eprint('trying to resolve request for current client')
        client_uuid = request.get(RequestSchema.uuid)
        range_record = self.ranges_for_client.get(client_uuid, None)
        if range_record is not None:
            entry = range_record[2]
            entry.resolved = True

    def __release_all_clients(self):
        print("releasing clients...")
        while len(self.sent_responses) > 0:
            self.lock.acquire()
            entry = self.sent_responses.popleft()
            time_diff = time.time() - entry.send_time
            if time_diff < ConcurrentServer.TIMEOUT:
                self.sent_responses.appendleft(entry)
                time_to_sleep = ConcurrentServer.TIMEOUT - time_diff
                self.lock.release()
                time.sleep(time_to_sleep)
            else:
                self.registered_clients.remove(entry.uuid)
                del self.ranges_for_client[entry.uuid]
                self.lock.release()
        print("released clients")

    def __close(self):
        eprint("closing server")
        self.is_running = False
        self.thread_pool.shutdown(False)
        self.sent_responses.clear()
        self.ranges_for_client.clear()
        self.registered_clients.clear()
        socket.socket(socket.AF_INET, socket.SOCK_STREAM).connect(self.address)
        self.socket.close()
        eprint("closed socket")

    @synchronized
    def __init_shutdown(self):
        if self.is_shutting:
            return

        self.is_shutting = True
        eprint('shutdown initiated')
        self.shutdown_thread = threading.Thread(target=self.shutdown)
        self.shutdown_thread.start()