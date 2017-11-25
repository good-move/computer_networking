import React from 'react';

import styles from '../styles/login.scss';

const USERNAME_PLACEHOLDER = "Select username...";


export default class LoginForm extends React.Component {

    constructor(props){
        super(props);

        this.state = {
            username: '',
            isUsernameFormatValid: true
        };
    }

    onLoginSubmit(event) {
        event.preventDefault();
        if (this.state.isUsernameFormatValid) {
            this.props.onLoginSubmit(this.state.username);
        }
    }

    onUsernameChange(event) {
        let isUsernameValid = true;
        const username = event.target.value.trim();

        if (!username.match(/^\w+$/)) {
            isUsernameValid = false;
        }

        this.setState({
            isUsernameFormatValid: isUsernameValid,
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
                {!this.state.isUsernameFormatValid &&
                <div className={styles.usernameFormatDescription}>
                    Username must:
                    <ul>
                        <li>consist of English letters and an underscore (A-Za-z_)</li>
                        <li>be at least 8 letters long</li>
                    </ul>
                </div>
                }
            </div>
        );
    }
}
