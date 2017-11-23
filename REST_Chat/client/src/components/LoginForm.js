import React from 'react';

const USERNAME_PLACEHOLDER = "USERNAME...";


export default class LoginForm extends React.Component {

    constructor(props){
        super(props);

        this.state = {
            username: USERNAME_PLACEHOLDER
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
            <div className={"loginFormHolder"}>
                <form className={"loginForm"}>
                    <input
                        className={"usernameInput"}
                        type={"text"}
                        placeholder={USERNAME_PLACEHOLDER}
                        value={this.state.username}
                        onChange={this.onUsernameChange.bind(this)}
                    />
                    <input
                        className={"submitLoginButton"}
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
