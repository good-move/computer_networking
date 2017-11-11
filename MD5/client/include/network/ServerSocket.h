//
// Created by alexey on 11.11.17.
//

#ifndef MD5_SERVERSOCKET_H
#define MD5_SERVERSOCKET_H


#include "InetSocketAddress.h"
#include "TcpSocket.h"

class TcpSocket;

class ServerSocket {
public:
    ServerSocket(InetSocketAddress address);
    ServerSocket(unsigned short port);
    ~ServerSocket();

    int SetOption();
    int Bind();
    TcpSocket Accept() const;
    int Close();

    int GetDescriptor() const;
    InetSocketAddress GetAddress() const;

    static const std::string LOCALHOST;
    static const int NO_FLAGS = 0;

private:
    void InitSocket();
    int Listen() const;

    int socketDescriptor;
    InetSocketAddress socketAddress;
    static const int IP_PROTOCOL = 0;
    static const int SUCCESS = 0;
    static const int ERROR = -1;

};


#endif //MD5_SERVERSOCKET_H
