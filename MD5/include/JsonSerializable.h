//
// Created by alexey on 06.11.17.
//

#ifndef MD5_JSONSERIALIZABLE_H
#define MD5_JSONSERIALIZABLE_H

#include "../lib/json.hpp"


/*
 * Interface for json serializable objects
 */
class JsonSerializable {
public:
  virtual std::string ToJson() const = 0;
  virtual void FromJson(const std::string& json) = 0;
};


#endif //MD5_JSONSERIALIZABLE_H
