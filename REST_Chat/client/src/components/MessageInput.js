import React from 'react';

import styles from '../styles/message_view.scss';

const MSG_INPUT_FIELD_ID = "messageInputField";

export default class MessageInput extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            message: ''
        }
    }

    componentDidMount() {
        document.getElementById(MSG_INPUT_FIELD_ID).addEventListener('keydown', this.onEnterPressed.bind(this));
    }

    componentWillUnmount() {
        document.getElementById(MSG_INPUT_FIELD_ID).removeEventListener('keydown', this.onEnterPressed.bind(this));
    }

    onMessageContentChange(event) {
        this.setState({
            message: event.target.value
        });
    }

    onEnterPressed(event) {
        console.log("key pressed");
        if (event.keyCode === 13 && !event.shiftKey) {
            this.onSendMessage(event);
        }
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
                        id={MSG_INPUT_FIELD_ID}
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