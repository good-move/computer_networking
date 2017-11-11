#include <iostream>

#include "include/PermutationGenerator.h"
#include "include/network/TcpSocket.h"
#include "include/Client.h"

#include <bits/unique_ptr.h>
#include <iostream>
#include <vector>


using namespace std;

const vector<char> DEFAULT_ALPHABET = {'A', 'C', 'G', 'T'};

const int CLIENT_PORT = 3000;
const int SERVER_PORT = 5000;


int main()
{
  Client client(CLIENT_PORT, TcpSocket::LOCALHOST, SERVER_PORT);
  unique_ptr<PermutationGenerator> permGen(new PermutationGenerator(DEFAULT_ALPHABET));
  client.InitMd5Cracker(permGen.get());
  client.Register();

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