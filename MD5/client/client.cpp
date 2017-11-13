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

const int EXPECTED_ARGS_COUNT = 4;
const int PROGRAM_NAME_ARG_INDEX = 0;
const int CLIENT_PORT_ARG_INDEX = 1;
const int SERVER_PORT_ARG_INDEX = 3;

void PrintHelp(const char* programName) {
  cout << "Usage: " << programName << " client_port server_address server_port" << endl;
}

int ParseInt(const char *stringValue);

int main(int argc, char** argv) {

  if (argc < EXPECTED_ARGS_COUNT) {
    PrintHelp(argv[PROGRAM_NAME_ARG_INDEX]);
    exit(EXIT_SUCCESS);
  }

  unsigned short clientPort = 0, serverPort = 0;
  try {
    clientPort = (unsigned short) ParseInt(argv[CLIENT_PORT_ARG_INDEX]);
    serverPort = (unsigned short) ParseInt(argv[SERVER_PORT_ARG_INDEX]);
  } catch (range_error& e) {
    cerr << e.what() << endl;
    exit(EXIT_FAILURE);
  }

  try {
    cerr << "Starting client" << endl;
    unique_ptr<PermutationGenerator> permGen(new PermutationGenerator(DEFAULT_ALPHABET));
    Client client(clientPort, TcpSocket::LOCALHOST, serverPort, permGen.get());
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
        client.Reconnect();
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

int ParseInt(const char *stringValue) {
  static const int BASE = 10;
  static const char STRING_TERMINATOR = '\0';
  char* validStringEnd;

  errno = 0;
  long result = strtol(stringValue, &validStringEnd, BASE);

  if (errno == ERANGE || errno == EINVAL) {
    throw range_error("Failed to parse string: value is out of range");
  }

  if ((*validStringEnd) != STRING_TERMINATOR) {
    throw range_error("Failed to parse string: non-digit characters found");
  }

  return (int) result;
}