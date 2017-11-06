#include <iostream>
#include "include/PermutationGenerator.h"
#include "include/Md5Cracker.h"
#include "include/request/PostAnswerRequest.h"

#include <bits/unique_ptr.h>

using namespace std;

const vector<char> DEFAULT_ALPHABET = {'A', 'C', 'G', 'T'};

int main()
{
  /*
   * Create all auxiliary objects
   * Connect to server and request brute force range
   * Brute force
   * get result
   */

//#define __A

#ifdef __A
  unique_ptr<PermutationGenerator> permGenPtr{new PermutationGenerator(DEFAULT_ALPHABET)};
  Md5Cracker cracker(permGenPtr.get(), "ff9c072d42a94d0a5112613019b54eae");
  cracker.SetRange("AA", "AAA");
  cracker.Crack();
  if (cracker.MatchFound()) {
    cout << "Hash cracked! Target string: " << cracker.GetHashOrigin() << endl;
  } else {
    cout << "No match found" << endl;
  }

#endif

  PostAnswerRequest request{"123", "AACG"};
  string json = request.ToJson();

  cout << json << endl;

  return 0;
}