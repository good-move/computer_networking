//
// Created by alexey on 06.11.17.
//

#ifndef MD5_REGISTERREQUEST_H
#define MD5_REGISTERREQUEST_H

#include "Request.h"

/*
 * Get server generated uuid and md5 hash to crack
 */
struct RegisterRequest : public Request {

    virtual int GetCode() const override final {
      return RegisterRequest::REQUEST_CODE;
    }

    virtual std::string ToJson() const override {
      nlohmann::json json = {
              { "code", this->GetCode() }
      };

      return json.dump();
    };

    virtual void FromJson(const std::string& json) override {}

private:
    static const int REQUEST_CODE = REGISTER;
};

#endif //MD5_REGISTERREQUEST_H
