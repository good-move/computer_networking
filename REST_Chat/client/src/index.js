import React from 'react';
import ReactDOM from 'react-dom';
import { BrowserRouter, Switch, Route, Redirect } from 'react-router-dom';

import styles from './styles/main.scss';

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

        this.setState({
            isLoggedIn: true,
            authToken: token
        });
    }

    render() {
        return (
            <Switch>
                <Route exact path='/' render={() => (
                        this.state.isLoggedIn ? (
                            <Redirect to={'/chat'} />
                        ) : (
                            <LoginPage onTokenReceived={this.onAuthTokenReceived.bind(this)} />
                        )
                    )
                } />
                <Route path='/chat' component={ChatPage} />
                <Route component={NotFoundPage} />
            </Switch>
        );
    }

}


ReactDOM.render(
    <BrowserRouter>
        <App />
    </BrowserRouter>,
    document.getElementById('app')
);
