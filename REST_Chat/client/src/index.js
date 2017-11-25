import React from 'react';
import ReactDOM from 'react-dom';
import { BrowserRouter, Switch, Route, Redirect } from 'react-router-dom';

import styles from './styles/main.scss';

import API from './server-api.js';
import LoginPage from './pages/login.js';
import ChatPage from './pages/chat.js';
import NotFoundPage from './pages/not_found.js';


class App extends React.Component {

    constructor(props) {
        super(props);

        this.state = {
            isLoggedIn: false,
            authToken: null
        }
    }

    onAuthTokenReceived(token) {
        console.log("Auth. token received: " + token);
        API.__setAuthToken(token);
        this.setState({
            isLoggedIn: true,
            authToken: token
        });
    }

    onLogout() {
        API.session.logout(this.state.authToken)
            .then(response => this.setState({
                isLoggedIn: false,
                authToken: null
            }))
            .catch(error => console.error(error))
    }

    render() {
        return (
            !this.state.isLoggedIn ? (
                <LoginPage onTokenReceived={this.onAuthTokenReceived.bind(this)} />
            ) : (
                <ChatPage onLogout={this.onLogout.bind(this)} />
            )
        );
    }

}


ReactDOM.render(
    <BrowserRouter>
        <App />
    </BrowserRouter>,
    document.getElementById('app')
);
