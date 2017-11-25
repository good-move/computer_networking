import React from 'react';

import styles from '../styles/chat.scss';

import UserList from '../components/UserList.js';
import MessageInput from '../components/MessageInput.js';
import MessageList from '../components/MessageList.js';

import API from '../server-api.js';

const APP_NAME = "REST CHAT";
const POLL_INTERVAL = 1000;

export default class ChatPage extends React.Component {


    constructor(props) {
        super(props);
        this.state = {
            userList: [],
            messageList: []
        };

        this.messageListUpdater = null;
        this.userListUpdater = null;
    }

    updateUserList() {
        API.users.getOnlineUsers()
            .then(response => {
                const userList = response.data.users;
                this.setState({ userList: userList });
            })
            .catch(error => {
                console.log(error);
            })
    }

    updateMessageList() {
        API.messages.getList()
            .then(response => {
                this.setState({
                    messageList: response.data.messages
                });
            })
            .catch(error => {
                console.log(error);
            })
    }

    componentDidMount() {
        this.updateMessageList();
        this.updateUserList();
        this.messageListUpdater = setInterval(this.updateMessageList.bind(this), POLL_INTERVAL);
        this.userListUpdater = setInterval(this.updateUserList.bind(this), POLL_INTERVAL);
    }

    componentWillUnmount() {
        this.stopUpdaters()
    }

    postMessage(message) {
        API.messages.create(message)
            .then(response => {
                console.log("Message sent!");
            })
            .catch(error => console.log(error));
    }

    onLogout(event) {
        this.stopUpdaters();
        this.props.onLogout();
    }

    stopUpdaters() {
        clearInterval(this.messageListUpdater);
        clearInterval(this.userListUpdater);
    }

    render() {
        return (
            <div className={styles.chatPage}>
                <div className={styles.chatHeader}>
                    <h1>{APP_NAME}</h1>
                    <div className={styles.controls}>
                        <input
                            className={styles.logoutButton}
                            type={"button"}
                            value={"Log out"}
                            onClick={this.onLogout.bind(this)}
                        />
                    </div>
                </div>
                <div className={styles.chatBody}>
                    <div className={styles.userListColumn}>
                        <UserList userList={this.state.userList} />
                    </div>
                    <div className={styles.chatViewColumn}>
                        <div className={styles.messageListView}>
                            <MessageList
                                messageList={this.state.messageList}
                            />
                        </div>
                        <div className={styles.messageInputView}>
                            <MessageInput onSendMessage={this.postMessage.bind(this)} />
                        </div>
                    </div>
                </div>
            </div>
        );
    }

}