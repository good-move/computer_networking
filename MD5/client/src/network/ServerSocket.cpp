//
// Created by alexey on 11.11.17.
//

#include "../../include/network/ServerSocket.h"

using namespace std;

ServerSocket::
ServerSocket(InetSocketAddress address)
        : socketAddress(address) {
  InitSocket();
}

ServerSocket::
ServerSocket(unsigned short port)
        : socketAddress(port) {
  InitSocket();
}

ServerSocket::
~ServerSocket() {
  Close();
}

int
ServerSocket::
Close() {
  int code = close(socketDescriptor);
  if (code == ERROR) {
    perror("Failed to close socket");
    return code;
  }

  return SUCCESS;
}

void
ServerSocket::
InitSocket() {
  socketDescriptor = socket(AF_INET, SOCK_STREAM, IP_PROTOCOL);

  if (socketDescriptor == ERROR) {
    const string errorMsg = "Failed to create socket";
    perror(errorMsg.c_str());
    throw runtime_error(errorMsg);
  }
}

// ********************************************************************************
// **************************** Basic socket routines *****************************

int
ServerSocket::
Bind() {
  int result = bind(socketDescriptor, (sockaddr*)socketAddress.GetRawAddress(), sizeof(sockaddr_in));

  if (result == ERROR) {
    const string errorMsg = "Failed to bind socket";
    perror(errorMsg.c_str());
    throw runtime_error(errorMsg);
  }
  return Listen();
}

TcpSocket
ServerSocket::
Accept() const {
  sockaddr_in rawAddress;
  socklen_t socketStructSize = sizeof(sockaddr_in);
  int descriptor = accept(socketDescriptor, (sockaddr*)&rawAddress, &socketStructSize);

  TcpSocket socket(descriptor, &rawAddress);
  return socket;
}

InetSocketAddress
ServerSocket::
GetAddress() const {
  return socketAddress;
}

int
ServerSocket::
GetDescriptor() const {
  return socketDescriptor;
}

// ********************************************************************************
// **************************** Private functions *********************************

int
ServerSocket::
Listen() const {
  int result = listen(socketDescriptor, 1);
  if (result == ERROR) {
    perror("Failed to turn the socket into listening mode");
    return result;
  }
  return SUCCESS;
}