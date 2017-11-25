import React from 'react';

import styles from '../styles/message_view.scss';


const MessageListItem = ({ message }) => (
    <div className={styles.messageListItem}>
        <p>{message.username}</p>
        <p>{message.message}</p>
    </div>
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