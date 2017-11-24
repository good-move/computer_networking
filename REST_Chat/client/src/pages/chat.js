import React from 'react';

import UserList from '../components/UserList.js';



const APP_NAME = "REST CHAT";


export default class ChatPage extends React.Component {


    constructor(props) {
        super(props);
        this.state = {
            userList: []
        };
    }

    render() {
        return (
            <div className={"chatPage"}>
                <div className={"chatHeader"}>
                    <h1>{APP_NAME}</h1>
                    <div className={"controls"}>
                        <input
                            className={"logoutButton"}
                            type={"button"}
                            value={"Log out"}
                        />
                    </div>
                </div>
                <div className={"chatBody"}>
                    <div className={"userListColumn"}>
                        <UserList userList={this.state.userList} />
                    </div>
                    <div className={"chatViewColumn"}>
                        <div className={"messageListView"}>

                        </div>
                        <div className={"messageInputView"}>

                        </div>
                    </div>
                </div>
            </div>
        );
    }

}