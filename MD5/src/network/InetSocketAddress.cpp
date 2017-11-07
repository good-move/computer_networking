#include "../../include/network/InetSocketAddress.h"

std::string to_string(const InetSocketAddress& address) {
  auto addr_in = address.GetRawAddress();
  char stringAddress[15] = {0};
  const char* result = inet_ntop(AF_INET, &addr_in->sin_addr, stringAddress, 15);
  if (result == nullptr) {
    throw std::runtime_error("Failed to convert address to string");
  }

  std::stringstream ss;
  ss << "(" << stringAddress << ", " << addr_in->sin_port << ")";
  return ss.str();
}

std::ostream& operator<<(std::ostream& os, const InetSocketAddress& address) {
  os << to_string(address);
  return os;
};