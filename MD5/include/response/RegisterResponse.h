//
// Created by alexey on 08.11.17.
//

#ifndef MD5_REGISTERRESPONSE_H
#define MD5_REGISTERRESPONSE_H


#include "Response.h"
#include "../ResponseHandler.h"

struct RegisterResponse : public Response {

    virtual void handle(ResponseHandler& handler) const override {
      handler(*this);
    }

    virtual std::string ToJson() const override {
      nlohmann::json json = {
            { "status", "success" },
            { "uuid", uuid },
            { "hash", targetHash }
      };
      return json.dump();
    };

    virtual void FromJson(const std::string& json) override {
      nlohmann::json j = nlohmann::json::parse(json);
      uuid = j.at("uuid");
      targetHash = j.at("hash");
      status = ResponseStatus ::SUCCESS;
    };

    std::string uuid;
    std::string targetHash;

};


#endif //MD5_REGISTERRESPONSE_H
