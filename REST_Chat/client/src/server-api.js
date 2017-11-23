import axios from 'axios';

const BASE_URL = "http://localhost:5000";

const makeAsyncRequest = (url, request_method, params={}, headers={}, data={}) => {
    return axios({
        method: request_method,
        url: `${BASE_URL}/${url}`,
        headers: headers,
        params: params,
        data: data
    });
};


class MessageApi {

    constructor(){
        this.url = "messages";
    }

    getList() {
        return makeAsyncRequest(this.url, 'GET');
    }

    create(message) {
        const parameters = {
            message: message
        };

        return makeAsyncRequest(this.url, 'POST', parameters, headers)
    }

}

class UsersApi {

    constructor(){
        this.url = "users";
    }

    getOnlineUsers() {
        return makeAsyncRequest(this.url, 'GET');
    }

    getUser(userId) {
        return makeAsyncRequest(`${this.url}/${userId}`, 'GET');
    }

}

class SessionApi {

    login(username) {
        return makeAsyncRequest(
            "login",
            "POST",
            {},{},
            { username: username }
        );
    }

    logout() {
        return null;
    }

}

export default {
    messages: new MessageApi(),
    users: new UsersApi(),
    session: new SessionApi()
}