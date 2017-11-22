from pyrest.decorators import RouteController, POST, GET
from pyrest.http import HttpRequest, HttpResponse


@RouteController
class MessageController:

    @POST('/messages')
    def post_message(self, request: HttpRequest) -> HttpResponse:
        return HttpResponse()

    @GET('/messages')
    def get_message_list(self, request: HttpRequest) -> HttpResponse:
        return HttpResponse()