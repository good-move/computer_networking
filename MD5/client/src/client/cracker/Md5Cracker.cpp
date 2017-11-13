//
// Created by alexey on 05.11.17.
//

#include "../../../include/Md5Cracker.h"

using namespace std;

Md5Cracker::
Md5Cracker(PermutationGenerator* permGen, const std::string& hash) {
  if (permGen == nullptr) throw invalid_argument("permutation generator pointer cannot be null");
  permGen_ = permGen;
  targetHash_ = hash;
}

Md5Cracker::
~Md5Cracker() {}

void
Md5Cracker::
SetRange(const std::string& rangeStart, const std::string& rangeEnd)
{
  this->rangeStart = rangeStart;
  this->rangeEnd = rangeEnd;
}

bool
Md5Cracker::
MatchFound() const
{
  return this->matchFound_;
}

std::string
Md5Cracker::
GetHashOrigin() const
{
  return this->hashPrototype_;
}

void
Md5Cracker::
Crack() {
  permGen_->SetRange(rangeStart, rangeEnd);
  for (;permGen_->HasNext(); permGen_->GetNext()) {
    const auto& permutation = permGen_->GetCurrent();
    if (TestHashMatch(permutation)) {
      matchFound_ = true;
      hashPrototype_ = permutation;
      break;
    }
  }
}

bool
Md5Cracker::
TestHashMatch(const std::string& testString) const {
  unsigned char* md5Hash = MD5((unsigned char*)testString.c_str(), testString.length(), nullptr);

  static string md5String = "";
  md5String.reserve(32);
  md5String.clear();

  for (size_t i = 0; i < MD5_DIGEST_LENGTH; ++i) {
    md5String += "0123456789abcdef"[md5Hash[i] / 16];
    md5String += "0123456789abcdef"[md5Hash[i] % 16];
  }

  cerr << "Tested " << testString  << " for hash match ----- ";
  cerr << "Matched: " << (md5String == targetHash_ ? "true" : "false") << endl;

  return md5String == targetHash_;
}

