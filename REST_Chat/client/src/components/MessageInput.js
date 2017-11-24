import React from 'react';

import styles from '../styles/message_view.scss';

export default class MessageInput extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            message: ''
        }
    }

    onMessageContentChange(event) {
        const message = event.target.value.trim();
        console.log(message);
        this.setState({
            message: message
        });
    }

    onSendMessage(event) {
        event.preventDefault();
        const message = this.state.message;
        this.setState({
            message: ''
        }, () => this.props.onSendMessage(message));
    }

    render() {
        return (
            <div className={styles.messageInput}>
                <form onSubmit={this.onSendMessage.bind(this)}>
                    <textarea
                        className={styles.messageTextArea}
                        onChange={this.onMessageContentChange.bind(this)}
                        value={this.state.message}
                    />
                    <input
                        type={"submit"}
                        value={"Send"}
                        onSubmit={this.onSendMessage.bind(this)}
                    />
                </form>
            </div>
        );
    }

}