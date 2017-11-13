import socket
import json
import struct

from server.utils import eprint


class TcpJsonStream:

    MESSAGE_LENGTH_SIZE = 4

    """
        Reads JSON from socket, parses it into python dict 
        and returns it 
         
        :param sock - socket to read message from
        
        :return dict - decoded JSON
    """
    @staticmethod
    def read_message(sock: socket.socket):
        eprint("Reading request")

        # read message length
        data = sock.recv(TcpJsonStream.MESSAGE_LENGTH_SIZE)
        if not data:
            print("no data received")
            return

        (message_length, *_) = struct.unpack('i', data)

        if message_length < 0:
            raise ValueError("Message length cannot be a negative number")

        # read json and decode into a dict
        data = sock.recv(message_length)
        if not data:
            eprint("no data received")
            raise ValueError("No JSON object is received")
        data = str(data, 'ascii')
        eprint(str(json.loads(data)))
        # print("JSON string ", str(json_string))
        return json.loads(data)



    """
        Encodes python dict into JSON and sends it over the socket 
        
        :param sock (socket.socket) - socket to send message to
        :param message (dict) - python dictionary, representing JSON message
    """
    @staticmethod
    def send_message(sock: socket.socket, message: dict):
        eprint("Sending response")
        eprint("Encoding message to JSON")

        json_message = json.dumps(message)

        eprint("Encoded json: " + json_message)
        message_length = len(json_message)

        eprint("Sending message length")
        sock.send(struct.pack('i', message_length))
        eprint("Sending JSON message")
        # sock.sendall(json_message)
        sock.send(bytes(json_message.encode('ascii')))
        eprint('Response sent')