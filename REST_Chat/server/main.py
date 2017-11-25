from pyrest.app import *
from pyrest.parser.tree_parser import TreeRouteParser
from src.controller.app_controller import *

server_address = ('', 5000)
app = PyRest(server_address=server_address, giRouteParserClass=TreeRouteParser)
app.run()

