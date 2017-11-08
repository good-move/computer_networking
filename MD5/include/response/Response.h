//
// Created by alexey on 06.11.17.
//

#ifndef MD5_RESPONSE_H
#define MD5_RESPONSE_H


#include "../ResponseHandler.h"

class Response : public JsonSerializable {

  public:

    virtual void handle(ResponseHandler& handler) const = 0;
    virtual std::string ToJson() const = 0;
    virtual void FromJson(const std::string& json) = 0;

  private:
};

#endif //MD5_RESPONSE_H
