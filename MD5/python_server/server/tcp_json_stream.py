import socket
import json


class TcpJsonStream:

    MESSAGE_LENGTH_SIZE = 8

    """
        Reads JSON from socket, parses it into python dict 
        and returns it 
         
        :param sock - socket to read message from
        
        :return dict - decoded JSON
    """
    @staticmethod
    def read_message(sock: socket.socket):
        # read message length
        message_length = sock.recv(TcpJsonStream.MESSAGE_LENGTH_SIZE)

        if message_length < 0:
            raise ValueError("Message length cannot be a negative number")

        # read json and decode into a dict
        data = str(sock.recv(message_length))
        print(data)
        return json.loads(data)

    """
        Encodes python dict into JSON and sends it over the socket 
        
        :param sock (socket.socket) - socket to send message to
        :param message (dict) - python dictionary, representing JSON message
    """
    @staticmethod
    def send_message(sock: socket.socket, message: dict):
        json_message = json.dumps(message)
        message_length = len(json_message)

        sock.send(message_length)
        sock.sendall(json_message)