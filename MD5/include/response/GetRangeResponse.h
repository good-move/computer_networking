//
// Created by alexey on 08.11.17.
//

#ifndef MD5_GETRANGERESPONSE_H
#define MD5_GETRANGERESPONSE_H

#include "Response.h"
#include "../ResponseHandler.h"

class GetRangeResponse : public Response {

    virtual void handle(ResponseHandler& handler) const override {
      handler(*this);
    }

    virtual std::string ToJson() const override { return ""; };
    virtual void FromJson(const std::string& json) override {};
};

#endif //MD5_GETRANGERESPONSE_H
