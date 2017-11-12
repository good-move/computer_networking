//
// Created by alexey on 08.11.17.
//

#ifndef MD5_CLIENT_H
#define MD5_CLIENT_H


#include "Md5Cracker.h"
#include "network/TcpJsonStream.h"
#include "network/TcpSocket.h"
#include "ResponseHandler.h"
#include "response/PostAnswerResponse.h"
#include "response/ErrorResponse.h"
#include "response/RegisterResponse.h"
#include "response/GetRangeResponse.h"

#include <bits/unique_ptr.h>

class Client : public ResponseHandler {

  public:
    Client(const unsigned short clientPort,
           const std::string& serverAddress,
           unsigned short serverPort,
           PermutationGenerator* permGen
    );
    virtual ~Client();

    /*
     * Sends Register message to the server and receives
     * md5 hash to crack and client uuid
     */
    void Register();
    void FetchNextAttackRange();
    bool FindHashOrigin();
    void SendAnswer();


    bool IsRunning();
    bool HashOriginFound();
    std::string GetAnswer();


    virtual void operator()(const ErrorResponse& error);
    virtual void operator()(const RegisterResponse& response);
    virtual void operator()(const GetRangeResponse& response);
    virtual void operator()(const PostAnswerResponse& response);

  private:

    std::unique_ptr<TcpJsonStream> jsonStream;
    std::unique_ptr<TcpSocket> socket = nullptr;
    std::unique_ptr<InetSocketAddress> serverAddress = nullptr;
    std::unique_ptr<Md5Cracker> md5Cracker = nullptr;
    PermutationGenerator* permGenerator = nullptr;

    std::string uuid;
    std::string targetHash;
    std::string hashOrigin;

    bool isRunning = true;
    bool hashOriginFound = false;

};


#endif //MD5_CLIENT_H
