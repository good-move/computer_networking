import React from 'react';

import styles from '../styles/message_view.scss';

const SESSION_INFO_TYPE = "notification";

const MessageListItem = ({ message }) => (
    <div className={styles.messageListItem}>
        {
            message.type === SESSION_INFO_TYPE ? (
                <SessionNotificationMessage message={message} />
            ) : (
                <TextMessage message={message} />
            )
        }
    </div>
);

const TextMessage = ({ message }) => (
    [
        <p key={0} className={styles.username}>{ message.username }</p>,
        <p key={1} className={styles.textMessageContent}>{ message.message }</p>
    ]
);

const SessionNotificationMessage = ({ message }) => (
    [
        <p key={0} className={styles.sessionNotification}><span>{message.username}</span>{" " + message.message }</p>
    ]
);


export default class MessageList extends React.Component {
    render() {
        const messageList = this.props.messageList.map(message =>
            <MessageListItem key={message.id} message={message} />
        );
        return (
            <div className={styles.messageList}>
                {messageList}
            </div>
        );
    }
}