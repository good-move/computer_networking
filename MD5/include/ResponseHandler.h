//
// Created by alexey on 08.11.17.
//

#ifndef MD5_RESPONSEHANDLER_H
#define MD5_RESPONSEHANDLER_H


#include "response/PostAnswerResponse.h"
#include "response/ErrorResponse.h"
#include "response/RegisterResponse.h"
#include "response/GetRangeResponse.h"

class ResponseHandler {
public :
  virtual void operator()(const ErrorResponse& errorResponse) = 0;
  virtual void operator()(const RegisterResponse& errorResponse) = 0;
  virtual void operator()(const GetRangeResponse& errorResponse) = 0;
  virtual void operator()(const PostAnswerResponse& errorResponse) = 0;
};


#endif //MD5_RESPONSEHANDLER_H
