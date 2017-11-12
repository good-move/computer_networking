#include <iostream>

#include "include/PermutationGenerator.h"
#include "include/network/TcpSocket.h"
#include "include/Client.h"
#include "include/request/RegisterRequest.h"

#include <bits/unique_ptr.h>
#include <iostream>
#include <vector>


using namespace std;

const vector<char> DEFAULT_ALPHABET = {'A', 'C', 'G', 'T'};

const int CLIENT_PORT = 3020;
const int SERVER_PORT = 7000;


int main()
{
  try {
    cout << "Starting client" << endl;
    unique_ptr<PermutationGenerator> permGen(new PermutationGenerator(DEFAULT_ALPHABET));
    Client client(CLIENT_PORT, TcpSocket::LOCALHOST, SERVER_PORT, permGen.get());
    client.Register();

    client.FetchNextAttackRange();
    client.FindHashOrigin();
    while (client.IsRunning()) {
      client.Reconnect();
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
      cout << "Shutting down." << endl;
    } else {
      cout << "Shutting down with no answer found." << endl;
    }

  } catch (runtime_error& e) {
    cerr << e.what() << endl;
  }

  return 0;
}