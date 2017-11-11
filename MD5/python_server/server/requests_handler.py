import socketserver


class RequestsHandler():

    @staticmethod
    def handle(request):
        request_code = request.code.get("code", -1)
        request_handler = {
            0: RequestsHandler._handle_registration_request,
            1: RequestsHandler
        }.get(code, )



    @staticmethod
    def _handle_registration_request(request):
        pass

    @staticmethod
    def _handle_get_next_range_request(request):
        pass

    @staticmethod
    def handle_unknown_request():
        pass



