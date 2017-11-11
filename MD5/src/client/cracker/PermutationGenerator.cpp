//
// Created by alexey on 05.11.17.
//

#include <stdexcept>
#include "../../../include/PermutationGenerator.h"

using namespace std;


// *************************************************************************************
// ********************************** Public Methods ***********************************
// *************************************************************************************

PermutationGenerator::
PermutationGenerator(const std::vector<char> alphabet) {
  this->alphabet = alphabet;
}

void
PermutationGenerator::
SetRange(const std::string &rangeStart, const std::string &rangeEnd) {
  this->rangeStart = this->GetIndexForString(rangeStart);
  this->rangeEnd = this->GetIndexForString(rangeEnd);
  this->currentPosition = this->rangeStart;
}

void
PermutationGenerator::
SetRangeStart(const std::string rangeStart) {
  this->rangeStart = this->GetIndexForString(rangeStart);
  this->currentPosition = this->rangeStart;
}

void
PermutationGenerator::
SetRangeEnd(const std::string rangeEnd) {
  this->rangeEnd = this->GetIndexForString(rangeEnd);
}

std::string
PermutationGenerator::
GetStringForIndex(size_t index)
{
  if (index == 0) return "";
  index--;

  size_t permLength = 1;
  size_t start = 0;
  size_t permsCount = alphabet.size();
  while (start + permsCount <= index) {
    start += permsCount;
    permsCount *= alphabet.size();
    permLength++;
  }

  string permutation = "";

  size_t rangeLength = 1ull << (2 * (permLength-1));
  while (permLength > 0) {
    size_t p = index - start;
    size_t k = p / rangeLength;
    start = start + k * rangeLength;
    permutation += alphabet[k];
    rangeLength /= alphabet.size();
    permLength--;
  }

  return permutation;
}

size_t
PermutationGenerator::
GetIndexForString(const std::string& str) {
  const size_t length = str.length();
  size_t index = 0;
  size_t permsCount = 1;

  for (size_t i = 0; i < length; ++i) {
    index += permsCount;
    permsCount *= alphabet.size();
  }

  size_t permLength = length;
  for (int i = 0; permLength > 0; ++i) {
    permsCount /= alphabet.size();
    index += permsCount * this->GetLetterIndex(str[i]);
    permLength--;
  }

  return index;
}

size_t
PermutationGenerator::
GetStringLengthForIndex(size_t index) {
  if (index == 0) return 0;
  index--;

  size_t permLength = 1;
  size_t start = 0;
  size_t permsCount = alphabet.size();
  while (start + permsCount <= index) {
    start += permsCount;
    permsCount *= alphabet.size();
    permLength++;
  }

  return permLength;
}


bool
PermutationGenerator::
HasNext() {
  return currentPosition < rangeEnd;
}

std::string&
PermutationGenerator::
GetNext() {
  currentPermutation = GetStringForIndex(currentPosition);
  currentPosition++;
  return currentPermutation;
}

std::string&
PermutationGenerator::
GetCurrent() {
  return this->currentPermutation;
}

// *************************************************************************************
// *********************************** Private Methods *********************************
// *************************************************************************************

size_t
PermutationGenerator::
GetLetterIndex(const char& letter) {
  for (size_t i = 0; i < alphabet.size(); ++i) {
    if (alphabet[i] == letter) {
      return i;
    }
  }

  throw runtime_error("No such letter in alphabet: " + letter);
}


