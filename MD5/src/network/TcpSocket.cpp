//
// Created by alexey on 07.11.17.
//

#include "../../include/network/TcpSocket.h"

using namespace std;

TcpSocket::
TcpSocket(const string& address, const unsigned short port)
        : socketAddress_(address, port) {
  InitSocket();
}

TcpSocket::
TcpSocket(const unsigned short port)
        : socketAddress_(port) {
  InitSocket();
}

TcpSocket::
~TcpSocket() {
  Close();
}

int
TcpSocket::
Close() {
  int code = close(socketDescriptor);
  if (code == ERROR) {
    perror("Failed to close socket");
    return code;
  }

  return SUCCESS;
}

// ********************************************************************************
// **************************** Basic socket routines *****************************

int
TcpSocket::
Bind() {
  int result = bind(socketDescriptor, (sockaddr*)socketAddress_.GetRawAddress(), sizeof(sockaddr_in));
  if (result == ERROR) {
    const string errorMsg = "Failed to bind socket";
    perror(errorMsg.c_str());
    Close();
    throw runtime_error(errorMsg);
  }
  return SUCCESS;
}

int
TcpSocket::
Connect(const string& address, const unsigned short port) const {
  InetSocketAddress remoteAddress(address, port);
  return ConnectToRemote(remoteAddress);
}

int
TcpSocket::
Connect(const InetSocketAddress &address) const {
  return ConnectToRemote(address);
}

int
TcpSocket::
ConnectToRemote(const InetSocketAddress &remoteAddress) const {
  int result = connect(socketDescriptor,
                       (const sockaddr*)remoteAddress.GetRawAddress(),
                       remoteAddress.GetRawSize()
  );
  if (result == ERROR) {
    const string errorMsg = "Failed to connect to address " + to_string(remoteAddress) + ")";
    perror(errorMsg.c_str());
    return result;
  }

  return SUCCESS;
}

int
TcpSocket::
Listen() const {
  int result = listen(socketDescriptor, 1);
  if (result == ERROR) {
    perror("Failed to turn the socket into listening mode");
    return result;
  }
  return SUCCESS;
}

TcpSocket
TcpSocket::
Accept() const {
  sockaddr_in rawAddress;
  socklen_t socketStructSize = sizeof(sockaddr_in);
  int descriptor = accept(socketDescriptor, (sockaddr*)&rawAddress, &socketStructSize);

  TcpSocket socket(descriptor, &rawAddress);
  return socket;
}

ssize_t
TcpSocket::
Send(const void* buffer, size_t bufferSize, int flags) const {
  ssize_t bytesSent = send(socketDescriptor, buffer, bufferSize, flags);
  if (bytesSent == ERROR) {
    perror("Failed to send buffer");
  }

  return bytesSent;
}

ssize_t
TcpSocket::
Receive(void* buffer, size_t bufferSize, int flags) const {
  ssize_t bytesReceived = recv(socketDescriptor, buffer, bufferSize, flags);
  if (bytesReceived == ERROR) {
    perror("Failed to send buffer");
  }
  return bytesReceived;
}


InetSocketAddress
TcpSocket::
GetAddress() const {
  return socketAddress_;
}

int
TcpSocket::
GetDescriptor() const {
  return socketDescriptor;
}

// ********************************************************************************
// **************************** Private functions *********************************

void
TcpSocket::
InitSocket() {
  socketDescriptor = socket(AF_INET, SOCK_STREAM, IP_PROTOCOL);

  if (socketDescriptor == ERROR) {
    const string errorMsg = "Failed to create socket";
    perror(errorMsg.c_str());
    throw runtime_error(errorMsg);
  }
}

/*
 * Private constructor used with Accept function to
 * conveniently receive and control incoming connections
 */
TcpSocket::
TcpSocket(const int descriptor, const sockaddr_in* address) : socketAddress_(address) {
  socketDescriptor = descriptor;
}
