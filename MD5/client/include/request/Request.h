//
// Created by alexey on 05.11.17.
//

#ifndef MD5_MESSAGE_H
#define MD5_MESSAGE_H

#include "../JsonSerializable.h"
#include "json.hpp"

#include <string>

enum Requests {
    REGISTER,
    GET_RANGE,
    POST_ANSWER
};

struct Request : JsonSerializable {
    Request() {}
    Request(const std::string& uuid) {
      this->uuid = uuid;
    }

    virtual std::string ToJson() const override = 0;
    virtual void FromJson(const std::string& json) override = 0;

    virtual int GetCode() const = 0;

    std::string GetUuid() const {
     return this->uuid;
    }

    void SetUuid(const std::string& uuid) {
      this->uuid = uuid;
    }


protected:
    std::string uuid;

};


/*
 * As recommended by nlohmann::json library, define functions for convenient
 * serialization with `T obj = json;` style.
 */
template <class T>
void to_json(const nlohmann::json& j, T& t) {
  static_assert(std::is_base_of<JsonSerializable, T>::value, "JsonSerializable");
  j = nlohmann::json::parse(t.ToJson());
}

template <class T>
void from_json(const nlohmann::json& j, T& t) {
  static_assert(std::is_base_of<JsonSerializable, T>::value, "T must extend JsonSerializable");
  t.FromJson(j.dump());
}


#endif //MD5_MESSAGE_H
