//
// Created by alexey on 06.11.17.
//

#ifndef MD5_POSTANSWERREQUEST_H_H
#define MD5_POSTANSWERREQUEST_H_H

#include "Request.h"

/*
 * Send a string, such that its md5 hash equals to the target
 * hash sent by the server
 */
struct PostAnswerRequest : public Request {
    PostAnswerRequest() {}
    PostAnswerRequest(const std::string& uuid, const std::string& answer)
            : Request(uuid), answer(answer) {}

    virtual int GetCode() const override final {
      return PostAnswerRequest::REQUEST_CODE;
    }

    virtual std::string ToJson() const override {
      nlohmann::json json = {
              { "requestCode", this->GetCode() },
              { "uuid",  this->uuid },
              { "answer", this->answer }
      };

      return json.dump();
    }

    // TODO: Add check for missing fields
    virtual void FromJson(const std::string& jsonString) override {
      nlohmann::json json = nlohmann::json::parse(jsonString);
      this->uuid = json.at("uuid").get<std::string>();
      this->answer = json.at("answer").get<std::string>();
    }

    std::string GetAnswer() {
      return answer;
    }

private:
    static const int REQUEST_CODE = POST_ANSWER;
    std::string answer;
};

#endif //MD5_POSTANSWERREQUEST_H_H
