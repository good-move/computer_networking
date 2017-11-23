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
            .then(response => {
                const auth_token = response.data.token;
                console.log("Received token: ", auth_token);
                this.setState({
                    isLoggedIn: true,
                    isPendingResponse: false
                }, () => this.props.onTokenReceived(auth_token));
            })
            .catch(error => {
                if (error.response) {
                    console.log(error.response.status);
                }
                this.setState({
                    isPendingResponse: false
                });
            });
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