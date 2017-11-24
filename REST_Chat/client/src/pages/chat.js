import React from 'react';

import UserList from '../components/UserList.js';
import styles from '../styles/chat.scss';
import API from '../server-api.js';

const APP_NAME = "REST CHAT";


export default class ChatPage extends React.Component {


    constructor(props) {
        super(props);
        this.state = {
            userList: []
        };
    }

    componentDidMount() {
        API.users.getOnlineUsers()
            .then(response => {
                const userList = response.data.users;
                console.log(userList);
                this.setState({ userList: userList });
            })
            .catch(error => {
                console.log(error);
            })
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
                        />
                    </div>
                </div>
                <div className={styles.chatBody}>
                    <div className={styles.userListColumn}>
                        <UserList userList={this.state.userList} />
                    </div>
                    <div className={styles.chatViewColumn}>
                        <div className={styles.messageListView}>

                        </div>
                        <div className={styles.messageInputView}>

                        </div>
                    </div>
                </div>
            </div>
        );
    }

}