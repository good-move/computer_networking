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

    virtual std::string ToJson() const override {
      nlohmann::json json = {
              { "status", "success" },
              { "start", rangeStart },
              { "end", rangeEnd }
      };
      return json.dump();
    };

    virtual void FromJson(const std::string& json) override {
      nlohmann::json j = nlohmann::json::parse(json);
      rangeStart = j.at("start");
      rangeEnd = j.at("end");
      this->status = ResponseStatus::SUCCESS;
    };

    std::string rangeStart;
    std::string rangeEnd;
};

#endif //MD5_GETRANGERESPONSE_H
