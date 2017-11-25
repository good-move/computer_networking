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

const REQUEST_METHODS = {
    get: 'GET',
    post: 'POST',
    put: 'PUT',
    update: 'UPDATE'
};

class Api {

    constructor(path) {
        this.path = path;
        this.auth_token = null
    }

    setAuthToken(token) {
        this.auth_token = token;
    }

    getAuthHeader() {
        return {
            "Authorization": `Authorization: Token ${this.auth_token}`
        }
    }
}

class MessageApi extends Api {

    constructor(path){
        super(path);
    }

    getList() {
        return makeAsyncRequest(this.path, REQUEST_METHODS.get, {}, this.getAuthHeader());
    }

    create(message) {
        const data = {
            message: message
        };

        return makeAsyncRequest(this.path, REQUEST_METHODS.post, {}, this.getAuthHeader(), data);
    }

}

class UsersApi extends Api {

    constructor(path) {
        super(path);
    }

    getOnlineUsers() {
        return makeAsyncRequest(
            this.path,
            REQUEST_METHODS.get, {},
            this.getAuthHeader()
        );
    }

    getUser(userId) {
        return makeAsyncRequest(
            `${this.path}/${userId}`,
            REQUEST_METHODS.get, {},
            this.getAuthHeader()
        );
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

    logout(auth_token) {
        return makeAsyncRequest(
            "logout",
            REQUEST_METHODS.get,
            {}, {
                "Authorization": `Authorization: Token ${auth_token}`
            }
        );
    }

}


const api = {
    messages: new MessageApi('messages'),
    users: new UsersApi('users'),
    session: new SessionApi(),
    __setAuthToken : token => {
        api.messages.setAuthToken(token);
        api.users.setAuthToken(token);
    }
};

export default api;