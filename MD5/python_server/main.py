import sys

from server.server import ConcurrentServer

EXPECTED_ARGS_COUNT = 3


def print_help():
    print("Usage: python3 " + sys.argv[0] + " port hash")


def main():
    if len(sys.argv) < EXPECTED_ARGS_COUNT:
        print_help()

    port = int(sys.argv[1])
    target_hash = str(sys.argv[2])

    print("starting server")
    server = ConcurrentServer(target_hash, '', port)
    server.start()
    print("server finished")

#     start server

if __name__ == "__main__":
    main()