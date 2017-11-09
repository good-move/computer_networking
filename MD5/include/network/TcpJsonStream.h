//
// Created by alexey on 08.11.17.
//

#ifndef MD5_TCPJSONSTREAM_H
#define MD5_TCPJSONSTREAM_H


#include "../JsonSerializable.h"
#include "../network/TcpSocket.h"

class TcpJsonStream {

  public:
    TcpJsonStream(TcpSocket& socket) : socket(socket) {}

    void Send(const JsonSerializable* request) const {
      const std::string requestJson = request->ToJson();
      size_t jsonLength = requestJson.length();

      auto bytesSent = socket.Send(
              &jsonLength, sizeof(jsonLength), TcpSocket::NO_FLAGS);
      if (bytesSent < 0) {
        throw std::runtime_error("Failed to send JSON length");
      }

      bytesSent = socket.Send(
              requestJson.c_str(), requestJson.length(), TcpSocket::NO_FLAGS);
      if (bytesSent < 0) {
        throw std::runtime_error("Failed to send JSON message");
      }

    }

    template <class SuccessType, class ErrorType>
    void Receive(JsonSerializable* response) const {
      ssize_t messageSize = 0;
      socket.Receive(&messageSize, sizeof(ssize_t), TcpSocket::NO_FLAGS);

      if (messageSize < 0) {
        throw std::runtime_error("Failed to receive message: message length cannot be a negative number");
      }

      char* jsonStringBuffer = new char[messageSize];
      socket.Receive(jsonStringBuffer, (size_t)messageSize, TcpSocket::NO_FLAGS);
      const std::string jsonString(jsonStringBuffer);
      delete[] jsonStringBuffer;

      const nlohmann::json json = nlohmann::json::parse(jsonString);

      if (json.count("status") == 0) {
        throw std::runtime_error("Failed to parse JSON: cannot figure out response type (no status field was found)");
      }

      std::string responseStatus = json.at("status");
      if (responseStatus == "success") {
        response = new SuccessType();
        response->FromJson(jsonString);
      } else if (responseStatus == "error") {
        response = new ErrorType();
        response->FromJson(jsonString);
      } else {
        throw std::runtime_error("Failed to parse JSON: unknown status type");
      }
    }

  private:
    TcpSocket& socket;
};


#endif //MD5_TCPJSONSTREAM_H
