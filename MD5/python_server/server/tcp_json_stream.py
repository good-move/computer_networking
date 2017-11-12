import socket
import json
import struct


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
        print("Reading message length")
        # read message length
        data = sock.recv(TcpJsonStream.MESSAGE_LENGTH_SIZE)
        if not data:
            print("no data received")
            return

        (message_length, *_) = struct.unpack('i', data)

        if message_length < 0:
            raise ValueError("Message length cannot be a negative number")

        print("Reading message of size " + str(message_length))
        # read json and decode into a dict
        data = sock.recv(message_length)
        if not data:
            print("no data received")
            raise ValueError("No JSON object is received")
        data = str(data, 'ascii')
        print(str(json.loads(data)))
        # print("JSON string ", str(json_string))
        return json.loads(data)



    """
        Encodes python dict into JSON and sends it over the socket 
        
        :param sock (socket.socket) - socket to send message to
        :param message (dict) - python dictionary, representing JSON message
    """
    @staticmethod
    def send_message(sock: socket.socket, message: dict):
        print("Endoding message to JSON")

        json_message = json.dumps(message)

        print("Encoded json: " + json_message)
        message_length = len(json_message)

        print("Sending message length")
        sock.send(struct.pack('i', message_length))
        print("Sending JSON message")
        # sock.sendall(json_message)
        sock.send(bytes(json_message.encode('ascii')))
        print('sent')