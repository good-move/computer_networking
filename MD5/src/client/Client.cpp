//
// Created by alexey on 08.11.17.
//

#include "../../include/network/TcpJsonStream.h"
#include "../../include/response/Response.h"
#include "../../include/request/RegisterRequest.h"
#include "../../include/Client.h"

using namespace std;

/*
 * Initializes client socket and server address and
 * connects to the server
 */
Client::
Client(const unsigned short clientPort,
       const std::string &serverAddress,
       unsigned short serverPort) {
  this->serverAddress.reset(new InetSocketAddress(serverAddress, serverPort));
  this->socket.reset(new TcpSocket(clientPort));
  this->socket->Bind();
  this->socket->Connect((*this->serverAddress));
  this->jsonStream.reset(new TcpJsonStream(*this->socket));
}

Client::
~Client() {}

void
Client::
InitMd5Cracker(PermutationGenerator* permGen) {
  md5Cracker.reset(new Md5Cracker(permGen, targetHash));
}

void
Client::
Register() {
  // if socket/cracker is not initialized, throw an error

  unique_ptr<Request> request(new RegisterRequest());
  jsonStream->Send(request.get());

  unique_ptr<Response> response = nullptr;
  jsonStream->Receive<RegisterResponse, ErrorResponse>(response.get());
  response->handle(*this);
}