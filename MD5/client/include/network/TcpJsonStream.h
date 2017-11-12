//
// Created by alexey on 08.11.17.
//

#ifndef MD5_TCPJSONSTREAM_H
#define MD5_TCPJSONSTREAM_H


#include "../JsonSerializable.h"
#include "TcpSocket.h"
#include "../response/Response.h"

class TcpJsonStream {

  using message_size_type = int;

  public:
    TcpJsonStream(TcpSocket& socket) : socket(socket) {}

    void Send(const JsonSerializable* request) const {
      const std::string requestJson = request->ToJson();
      message_size_type jsonLength = (message_size_type)requestJson.length();

      std::cerr << "Sending message length: " << jsonLength << std::endl;

      auto bytesSent = socket.Send(
              &jsonLength, sizeof(message_size_type), TcpSocket::NO_FLAGS);
      if (bytesSent < 0) {
        throw std::runtime_error("Failed to send JSON length");
      }

      bytesSent = socket.Send(
              requestJson.c_str(), (size_t)jsonLength, TcpSocket::NO_FLAGS);
      if (bytesSent < 0) {
        throw std::runtime_error("Failed to send JSON message");
      }

    }

    template <class SuccessType, class ErrorType>
    Response* Receive() const {
      message_size_type messageSize = 0;
      std::cerr << "Waint for message length" << std::endl;
      if (socket.Receive(&messageSize, sizeof(message_size_type), TcpSocket::NO_FLAGS) < 0) {
        throw std::runtime_error("Failed to receive message length");
      }

      std::cerr << "Length: " << messageSize << std::endl;

      if (messageSize < 0) {
        throw std::runtime_error("Failed to receive message: message length cannot be a negative number");
      }

      std::cerr << "Waiting for JSON response" << std::endl;
      char* jsonStringBuffer = new char[messageSize+1]();
      if (socket.Receive(jsonStringBuffer, (size_t)messageSize, TcpSocket::NO_FLAGS) < 0) {
        throw std::runtime_error("Failed to receive JSON response");
      }
      const std::string jsonString(jsonStringBuffer);
      delete[] jsonStringBuffer;

      std::cerr << "response: " << jsonString << std::endl;
      const nlohmann::json json = nlohmann::json::parse(jsonString);

      if (json.count("result") == 0) {
        throw std::runtime_error("Failed to parse JSON: cannot figure out response type (no status field was found)");
      }

      Response* response = nullptr;
      std::string responseStatus = json.at("result");
      if (responseStatus == "success") {
        std::cerr << "success response" << std::endl;
        response = new SuccessType();
        response->FromJson(jsonString);
      } else if (responseStatus == "error") {
        std::cerr << "error response" << std::endl;
        response = new ErrorType();
        response->FromJson(jsonString);
      } else {
        throw std::runtime_error("Failed to parse JSON: unknown status type");
      }

      return response;
    }

  private:
    TcpSocket& socket;
};


#endif //MD5_TCPJSONSTREAM_H
