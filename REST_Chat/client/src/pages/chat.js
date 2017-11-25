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
            usernames: new Map(),
            userList: [],
            messageList: []
        };

        this.messageListUpdater = null;
        this.userListUpdater = null;
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

    componentDidUpdate() {
        const messageListElement = document.getElementById("messageListView");
        messageListElement.scrollTop = messageListElement.scrollHeight;
    }

    updateUserList() {
        API.users.getOnlineUsers()
            .then(response => {
                this.setState({ userList: response.data.users });
            })
            .catch(error => {
                console.log(error);
            })
    }

    updateMessageList() {
        API.messages.getList()
            .then(response => {
                const currentList = this.state.messageList;
                let messageList = response.data.messages;

                if (currentList.length > 0 &&
                    currentList[0].id === messageList[0].id &&
                    currentList.length === messageList.length) {
                    return;
                }

                let usernames = this.state.usernames;
                let usernamePromises = [];
                messageList.forEach(message => {
                   if (!usernames.has(message.author_id)) {
                       usernamePromises.push(API.users.getUser(message.author_id));
                   }
                });

                Promise.all(usernamePromises)
                    .then(responses => {
                        responses.forEach(res => {
                            usernames.set(res.data.id, res.data.username);
                            console.log("username set: " + usernames.get(res.data.id))
                        });

                        messageList = messageList.map(message =>
                            Object.assign({}, message, { username: usernames.get(message.author_id) })
                        );

                        this.setState({
                            usernames: usernames,
                            messageList: messageList
                        });
                    })
                    .catch(error => {
                        this.checkRequireAuth(error);
                       console.error("Failed to load users information: " + error.toString());
                    });
            })
            .catch(error => {
                this.checkRequireAuth(error);
                console.log(error);
            })
    }

    postMessage(message) {
        API.messages.create(message)
            .then(response => {
                console.log("Message sent!");
            })
            .catch(error => console.log(error));
    }

    checkRequireAuth(error) {
        if (error.response) {
            const status = error.response.status;
            if (status === 403 || status === 401) {
                this.stopUpdaters();
                this.props.requireAuth();
            }
        }
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
                            value={"LOG OUT"}
                            onClick={this.onLogout.bind(this)}
                        />
                    </div>
                </div>
                <div className={styles.chatBody}>
                    <div className={styles.userListColumn}>
                        <UserList userList={this.state.userList} />
                    </div>
                    <div className={styles.chatViewColumn}>
                        <div className={styles.messageListView} id={"messageListView"}>
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