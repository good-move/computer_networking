import React from 'react';

import LoginForm from '../components/LoginForm.js';
import API from '../server-api.js';
import styles from '../styles/login.scss';

const APP_NAME = "REST CHAT";


class LoginPage extends React.Component {

    constructor(props) {
        super(props);

        this.state = {
            isLoggedIn: false,
            isPendingResponse: false,
            showUsernameTakenError: false
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
                    isPendingResponse: false,
                    showUsernameTakenError: error.response.status === 401
                });
            });
    }

    render() {
        return (
            <div className={styles.loginPage}>
                <div className={styles.appNameHolder}>
                    <h1 className={styles.appName}>{APP_NAME}</h1>
                </div>
                <LoginForm
                    disabled={this.state.isPendingResponse}
                    onLoginSubmit={this.onLogin.bind(this)}
                />
                {this.state.showUsernameTakenError &&
                <div className={styles.errorMessage}><p>Username is already taken</p></div>
                }
            </div>
        );
    }
}

export default LoginPage;