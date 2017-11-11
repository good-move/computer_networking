from concurrent.futures import ThreadPoolExecutor
from hashlib import md5
import socket
import uuid

from server.tcp_json_stream import TcpJsonStream
from server.message import *


class ConcurrentServer:

    THREADS_COUNT = 5

    def __init__(self, target_hash: str, host: str, port: int):
        self.thread_pool = ThreadPoolExecutor(ConcurrentServer.THREADS_COUNT)
        self.hash = target_hash

        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.bind((host, port))
        sock.listen(5)

        print("Listening on " + str(sock.getsockname()))

        self.socket = sock
        self.is_running = True
        self.answer_found = False
        self.tcp_json_stream = TcpJsonStream()
        self.registered_clients = set()

    def start(self):
        while self.is_running:
            client_socket, client_address = self.socket.accept()
            print(client_address)
            self.thread_pool.submit(self.__handle_request, client_socket)

    def close(self):
        # TODO: complete function
        pass

    def __handle_request(self, client_socket: socket.socket):
        print("New request: " + str(client_socket.getsockname()))
        # read json
        request = self.tcp_json_stream.read_message(client_socket)

        request_code = request.get("code", -1)
        # choose a handler to handle current request
        handlers_map = {
            0: self.__handle_registration_request,
            1: self.__handle_get_range_request,
            2: self.__handle_post_answer_request
        }

        if request_code not in handlers_map:
            self.__handle_unknown_request(client_socket, request)

        handlers_map.get(request_code, self.__handle_unknown_request)(client_socket, request)
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
        # get uuid hex string
        client_uuid = uuid.uuid4().hex
        self.registered_clients.add(client_uuid)
        response = SuccessResponseFactory.create_registration_response(client_uuid, self.hash)
        self.tcp_json_stream.send_message(client_socket, response)

    """
        Looks for the next free range, that can be handed out to a client
        and assign it the the current client

        :param client_socket - client socket, which is used to respond to the client
        :param request (dict) - GetRangeRequest, format of which is specified in the
        protocol documentation
    """
    def __handle_get_range_request(self, client_socket: socket.socket, request: dict):
        self.__validate_schema_and_reply_on_error(client_socket, request)

        # TODO: write hand out algorithm
        self.tcp_json_stream.send_message(
            client_socket,
            SuccessResponseFactory.create_get_range_response("A", "AA")
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
        self.__validate_schema_and_reply_on_error(client_socket, request)

        # check that answer is valid
        answer = request.get(RequestSchema.answer, None)

        if answer in None:
            self.tcp_json_stream.send_message(
                client_socket,
                ErrorResponseFactory.create(ErrorCodes.WRONG_ANSWER, "Missing answer field")
            )

        answer_hash = md5(answer).hexdigest()
        if answer_hash == self.hash:
            response = SuccessResponseFactory.create_post_answer_response()
            self.tcp_json_stream.send_message(client_socket, response)
            self.__set_answer(answer)
        else:
            error = ErrorResponseFactory.create(ErrorCodes.WRONG_ANSWER, "Wrong answer: " + answer)
            self.tcp_json_stream.send_message(client_socket, error)


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

    def __is_uuid_registered(self, uuid: str):
        # TODO: set mutex
        return uuid in self.registered_clients

    def __set_answer(self, answer: str):
        # TODO: set mutex
        self.answer = answer
        self.answer_found = True

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

        # make sure the client is registered
        if not self.__is_uuid_registered(request[RequestSchema.uuid]):
            self.tcp_json_stream.send_message(
                client_socket,
                ErrorResponseFactory.create(
                    ErrorCodes.UNRECOGNIZED_UUID,
                    "UUID is not registered"
                )
            )
