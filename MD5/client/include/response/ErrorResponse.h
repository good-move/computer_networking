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

    virtual std::string ToJson() const override {
      nlohmann::json json = {
          { "status", "error" },
          { "code", errorCode },
          { "message", errorMessage }
      };
      return json.dump();
    };

    virtual void FromJson(const std::string& json) override {
      nlohmann::json j = nlohmann::json::parse(json);
      errorCode = j.at("code");
      errorMessage = j.at("message");
      this->status = ResponseStatus::ERROR;
    };

    enum ErrorCode {
        MISSING_UUID,
        UNRECOGNIZED_UUID,
        OUT_OF_RANGES,
        WRONG_ANSWER
    };

};

#endif //MD5_ERRORRESPONSE_H
