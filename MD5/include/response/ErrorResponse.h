//
// Created by alexey on 06.11.17.
//

#ifndef MD5_ERRORRESPONSE_H
#define MD5_ERRORRESPONSE_H

#include <string>

#include "Response.h"
#include "../ResponseHandler.h"

struct ErrorResponse : public Response {
    int errorCode;
    std::string errorMessage;

    virtual void handle(ResponseHandler& handler) const override {
      handler(*this);
    }

    virtual std::string ToJson() const override { return ""; };
    virtual void FromJson(const std::string& json) override {};

};

#endif //MD5_ERRORRESPONSE_H
