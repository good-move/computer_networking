//
// Created by alexey on 07.11.17.
//

#ifndef MD5_TCPSOCKET_H
#define MD5_TCPSOCKET_H


#include "InetSocketAddress.h"
#include "ServerSocket.h"


// socket, bind, accept, listen, connect, set_sockopt
#include <sys/socket.h>
// sockaddr, sockaddr_in
#include <netinet/in.h>
// string
#include <string>
// memset
#include <cstring>
// cerr, cout, cin
#include <iostream>
// close
#include <unistd.h>


class TcpSocket {

  public:
    TcpSocket(const unsigned short port);
    TcpSocket(const std::string& address, const unsigned short port);
    ~TcpSocket();

    int SetOption();
    int SetReusable(bool reusable);
    int Bind();
    int Connect(const std::string& address, const unsigned short port) const;
    int Connect(const InetSocketAddress& address) const;
    int Close();
    ssize_t Send(const void* buffer, size_t bufferSize, int flags) const;
    ssize_t Receive(void* buffer, size_t bufferSize, int flags) const;

    int GetDescriptor() const;
    InetSocketAddress GetAddress() const;


    static const std::string LOCALHOST;
    static const int NO_FLAGS = 0;

    friend class ServerSocket;

  private:
    TcpSocket(const int descriptor, const sockaddr_in* address);

    void InitSocket();
    int ConnectToRemote(const InetSocketAddress& remoteAddress) const;

    int socketDescriptor;
    InetSocketAddress socketAddress_;
    static const int IP_PROTOCOL = 0;
    static const int SUCCESS = 0;
    static const int ERROR = -1;
};


#endif //MD5_TCPSOCKET_H
