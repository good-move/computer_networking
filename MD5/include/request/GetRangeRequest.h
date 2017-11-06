//
// Created by alexey on 06.11.17.
//

#ifndef MD5_GETRANGEREQUEST_H
#define MD5_GETRANGEREQUEST_H

#include "Request.h"

/*
 * Get starting and ending permutations
 * of brute force attack range
 */
struct GetRangeRequest : public Request {
    GetRangeRequest() {}
    GetRangeRequest(const std::string& uuid) : Request(uuid) {}

    virtual int GetCode() const override final {
      return GetRangeRequest::REQUEST_CODE;
    }

    virtual std::string ToJson() const override {
      nlohmann::json json = {
              { "requestCode", this->GetCode() },
              { "uuid",  this->uuid }
      };

      return json.dump();
    }


    virtual void FromJson(const std::string& jsonString) override {
      nlohmann::json json = nlohmann::json::parse(jsonString);
      this->uuid = json.at("uuid").get<std::string>();
    }

private:
    static const int REQUEST_CODE = GET_RANGE;
};

#endif //MD5_GETRANGEREQUEST_H
