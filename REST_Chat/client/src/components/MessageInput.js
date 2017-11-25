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
        this.setState({
            message: event.target.value
        });
    }

    onSendMessage(event) {
        event.preventDefault();
        const message = this.state.message.trim();
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
                        className={styles.sendMessageButton}
                        type={"submit"}
                        value={"SEND"}
                        onSubmit={this.onSendMessage.bind(this)}
                    />
                </form>
            </div>
        );
    }

}