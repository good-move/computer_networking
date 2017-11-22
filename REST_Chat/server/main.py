from pyrest.app import *
from pyrest.parser.tree_parser import TreeRouteParser
from src.controller.app_controller import *


app = PyRest(RouteParserClass=TreeRouteParser)
app.run()

