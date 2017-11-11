//
// Created by alexey on 08.11.17.
//

#ifndef MD5_POSTANSWERRESPONSE_H
#define MD5_POSTANSWERRESPONSE_H


#include "../ResponseHandler.h"
#include "Response.h"

struct PostAnswerResponse : public Response {

    virtual void handle(ResponseHandler& handler) const override {
      handler(*this);
    }

    virtual std::string ToJson() const override {
      nlohmann::json json = {
          { "status", "success" }
      };
      return json.dump();
    };

    virtual void FromJson(const std::string& json) override {
      this->status = ResponseStatus::SUCCESS;
    };

};


#endif //MD5_POSTANSWERRESPONSE_H
