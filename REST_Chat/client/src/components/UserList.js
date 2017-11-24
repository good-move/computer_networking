import { Component } from 'react';

import UserListItem from './UserListItem.js';

export default class UserList extends Component {

    render() {
        const userList = this.props.userList.map(user =>
            <UserListItem key={user.id} user={user} />
        );

        return (
            <div className={"userList"}>
                <div className={"userListHolder"}>
                    {userList}
                </div>
            </div>
        );
    }

}
