#include <iostream>
#include "include/PermutationGenerator.h"
#include "include/Md5Cracker.h"
#include "include/request/PostAnswerRequest.h"
#include "include/network/TcpSocket.h"
#include "include/Client.h"

#include <bits/unique_ptr.h>
#include <iostream>

using namespace std;

const vector<char> DEFAULT_ALPHABET = {'A', 'C', 'G', 'T'};

int main()
{
  Client client(4000, TcpSocket::LOCALHOST, 3000);
  unique_ptr<PermutationGenerator> permGen(new PermutationGenerator(DEFAULT_ALPHABET));
  client.InitMd5Cracker(permGen.get());
  client.Register();
  return 0;

  while (client.IsRunning()) {
    client.FetchNextAttackRange();
    client.FindHashOrigin();
  }

  if (client.HashOriginFound()) {
    cout << "Answer found: " << client.GetAnswer() << endl;
    try {
      client.SendAnswer();
    } catch (const runtime_error& e) {
      cerr << "Error while sending answer to server" << endl;
      cerr << e.what() << endl;
    }
    cerr << "Shutting down." << endl;
  } {
    cerr << "Shutting down with no answer found." << endl;
  }

  return 0;
}