class RequestSchema:
    uuid = "uuid"
    answer = "answer"


class Request:
    request_schema = RequestSchema()
    request_types = {

    }


class SuccessResponseFactory:
    result = "success"

    @staticmethod
    def create_registration_response(uuid, hash):
        return {
            **SuccessResponseFactory.__get_success_response(),
            "uuid": uuid,
            "hash": hash
        }

    @staticmethod
    def create_post_answer_response():
        return {
            **SuccessResponseFactory.__get_success_response()
        }

    @staticmethod
    def create_get_range_response(range_start, range_end):
        return {
            **SuccessResponseFactory.__get_success_response(),
            "start": range_start,
            "end": range_end,
        }

    @staticmethod
    def __get_success_response():
        return {
            "result": SuccessResponseFactory.result
        }


class ErrorResponseFactory:
    result = "error"

    @staticmethod
    def create(error_code, error_message):
        return {
            "result": ErrorResponseFactory.result,
            "code": error_code,
            "message": error_message
        }


class ErrorCodes:
    MISSING_UUID = 0
    UNRECOGNIZED_UUID = 1
    OUT_OF_RANGES = 2
    WRONG_ANSWER = 3
    UNKNOWN_REQUEST_CODE = 4
