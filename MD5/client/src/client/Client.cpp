//
// Created by alexey on 08.11.17.
//

#include "../../include/network/TcpJsonStream.h"
#include "../../include/response/Response.h"
#include "../../include/request/RegisterRequest.h"
#include "../../include/Client.h"
#include "../../include/request/GetRangeRequest.h"
#include "../../include/request/PostAnswerRequest.h"

using namespace std;

/*
 * Initializes client socket and server address and
 * connects to the server
 */
Client::
Client(const unsigned short clientPort,
       const std::string &serverAddress,
       unsigned short serverPort,
       PermutationGenerator* permGen) {
  this->serverAddress.reset(new InetSocketAddress(serverAddress, serverPort));
  this->socket.reset(new TcpSocket(clientPort));
  this->socket->SetReusable(true);
  this->socket->Bind();
  if (this->socket->Connect((*this->serverAddress)) == -1) {
    socket->Close();
    throw runtime_error("Failed to connect to server");
  }
  this->permGenerator = permGen;
  this->jsonStream.reset(new TcpJsonStream(*this->socket));
}

Client::
~Client() {}

void
Client::
Register() {
  // TODO if socket/cracker is not initialized, throw an error

  unique_ptr<Request> request(new RegisterRequest());
  cerr << "Sending request" << endl;
  jsonStream->Send(request.get());

  unique_ptr<Response> response = nullptr;
  cerr << "Waiting for response" << endl;
  response.reset(jsonStream->Receive<RegisterResponse, ErrorResponse>());
  cerr << "Received response" << endl;
  response->handle(*this);
}

void
Client::
FetchNextAttackRange() {
  unique_ptr<Request> request(new GetRangeRequest{this->uuid});
  cerr << "Sending Range request" << endl;
  jsonStream->Send(request.get());

  unique_ptr<Response> response = nullptr;
  cerr << "Waiting for response" << endl;
  response.reset(jsonStream->Receive<GetRangeResponse, ErrorResponse>());
  cerr << "Received response" << endl;
  response->handle(*this);
}

void
Client::
SendAnswer() {
  unique_ptr<Request> request(new PostAnswerRequest{this->uuid, this->hashOrigin});
  jsonStream->Send(request.get());

  unique_ptr<Response> response = nullptr;
  response.reset(jsonStream->Receive<PostAnswerResponse, ErrorResponse>());
  response->handle(*this);
}

bool
Client::
FindHashOrigin() {
  md5Cracker->Crack();
  if (md5Cracker->MatchFound()) {
    cerr << "finishing work" << endl;
    hashOrigin = md5Cracker->GetHashOrigin();
    hashOriginFound = true;
    isRunning = false;
    return true;
  }
  return false;
}

bool
Client::
IsRunning() {
  return isRunning;
}

bool
Client::
HashOriginFound() {
  return hashOriginFound;
}

std::string
Client::
GetAnswer() {
  return hashOrigin;
}

// *************************************************************************
// ****************** ResponseHandler Interface overloads ******************

void
Client::
operator()(const ErrorResponse& error) {
  cerr << "Error. ErrorCode: " << error.errorCode;
  cerr << " Message: " << error.errorMessage;

  switch (error.errorCode) {
    case ErrorResponse::ErrorCode::OUT_OF_RANGES:
      isRunning = false;
      break;
    case ErrorResponse::ErrorCode::MISSING_UUID:
      break;
    case ErrorResponse::ErrorCode::UNRECOGNIZED_UUID:
      break;
    case ErrorResponse::ErrorCode::WRONG_ANSWER:
      throw runtime_error("Server responded with 'Wrong Answer' message");
    default: throw runtime_error("Unknown error code: " + to_string(error.errorCode));
  }
}

void
Client::
operator()(const RegisterResponse& response) {
  cerr << "Registered successfully" << endl;
  targetHash = response.targetHash;
  uuid = response.uuid;
  md5Cracker.reset(new Md5Cracker(permGenerator, targetHash));
}

void
Client::
operator()(const GetRangeResponse& response) {
  md5Cracker->SetRange(response.rangeStart, response.rangeEnd);
}

void
Client::
operator()(const PostAnswerResponse& response) {

}
