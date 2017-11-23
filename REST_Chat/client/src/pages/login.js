import React from 'react';

import LoginForm from '../components/LoginForm.js';
import API from '../server-api.js';

const APP_NAME = "REST CHAT";


class LoginPage extends React.Component {

    constructor(props) {
        super(props);

        this.state = {
            isLoggedIn: false,
            isPendingResponse: false
        };
    }

    onLogin(username) {
        console.log(username);
        this.setState({
            isPendingResponse: true
        }, () => this.sendLoginRequest(username));
    }

    sendLoginRequest(username) {
        API.session.login(username)
            .then(response => console.log(response))
            .catch(error => console.log(error));
    }

    render() {
        return (
            <div className={"loginPage"}>
                <div className={"appNameHolder"}>
                    <h1 className={"appName"}>{APP_NAME}</h1>
                </div>
                <LoginForm
                    disabled={this.state.isPendingResponse}
                    onLoginSubmit={this.onLogin.bind(this)}
                />
            </div>
        );
    }
}

export default LoginPage;