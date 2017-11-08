//
// Created by alexey on 08.11.17.
//

#ifndef MD5_RESPONSEHANDLER_H
#define MD5_RESPONSEHANDLER_H



class ResponseHandler {
public :
  virtual void operator()(const class ErrorResponse& errorResponse) = 0;
  virtual void operator()(const class RegisterResponse& errorResponse) = 0;
  virtual void operator()(const class GetRangeResponse& errorResponse) = 0;
  virtual void operator()(const class PostAnswerResponse& errorResponse) = 0;
};


#endif //MD5_RESPONSEHANDLER_H
