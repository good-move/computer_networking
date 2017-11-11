//
// Created by alexey on 07.11.17.
//

#ifndef MD5_SOCKETADDRESSV4_H
#define MD5_SOCKETADDRESSV4_H


#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <iostream>
#include <sstream>
#include <cstring>
#include <string>

class InetSocketAddress;


class InetSocketAddress {
  public:
    InetSocketAddress(const std::string& address, const unsigned short port) {
      in_addr binaryAddress;

      int result = inet_pton(AF_INET, address.c_str(), &binaryAddress);
      if (result == ERROR) {
        const std::string errorMsg = "Failed to parse host address";
        perror(errorMsg.c_str());
        throw std::runtime_error(errorMsg);
      }

      InitSocketAddress(binaryAddress.s_addr, port);
    }

    InetSocketAddress(const unsigned short port) {
      InitSocketAddress(INADDR_ANY, port);
    }

    InetSocketAddress(const sockaddr_in* rawAddress) {
      if (rawAddress == nullptr) throw std::runtime_error("rawAddress cannot be NULL");
      InitSocketAddress(rawAddress->sin_addr.s_addr, rawAddress->sin_port);
    }

    const sockaddr_in* GetRawAddress() const {
      return &address;
    }

    socklen_t GetRawSize() const {
      return (socklen_t) sizeof(sockaddr_in);
    }

    unsigned short GetPort() const {
      return (address.sin_port);
    }

private:

    void InitSocketAddress(in_addr_t address, const unsigned short port) {
      memset(&this->address, 0, GetRawSize());
      this->address.sin_family = AF_INET;
      this->address.sin_port = port;
      this->address.sin_addr.s_addr = address;
    }

    sockaddr_in address;

    static const int ERROR = -1;

};

std::string to_string(const InetSocketAddress& address);
std::ostream& operator<<(std::ostream& os, const InetSocketAddress& address);

#endif //MD5_IPV4ADDRESS_H
