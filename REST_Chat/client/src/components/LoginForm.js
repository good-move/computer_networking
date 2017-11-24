import React from 'react';

import styles from '../styles/login.scss';

const USERNAME_PLACEHOLDER = "Select username...";


export default class LoginForm extends React.Component {

    constructor(props){
        super(props);

        this.state = {
            username: ''
        };
    }

    onLoginSubmit(event) {
        event.preventDefault();

        const username = this.state.username.trim();
        if (username.match(/^\w+$/) !== null) {
            this.props.onLoginSubmit(username);
        } else {
            window.alert("Username must contain only digits, English letters or _");
        }
    }

    onUsernameChange(event) {
        const username = event.target.value.trim();
        this.setState({
            username: username
        });
    }

    render() {
        return (
            <div className={styles.loginFormHolder}>
                <form className={styles.loginForm} onSubmit={this.onLoginSubmit.bind(this)}>
                    <input
                        className={styles.usernameInput}
                        type={"text"}
                        placeholder={USERNAME_PLACEHOLDER}
                        value={this.state.username}
                        onChange={this.onUsernameChange.bind(this)}
                        autoFocus
                    />
                    <input
                        className={styles.submitLoginButton}
                        type={"submit"}
                        value={"Log in"}
                        onClick={this.onLoginSubmit.bind(this)}
                        disabled={this.props.disabled}
                    />
                </form>
            </div>
        );
    }
}
