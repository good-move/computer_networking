import React from 'react';

import UserListItem from './UserListItem.js';
import styles from '../styles/userlist.scss';

export default class UserList extends React.Component {

    render() {
        const userList = this.props.userList.map(user =>
            <UserListItem key={user.id} user={user} />
        );

        return (
            <div className={styles.userList}>
                <div className={styles.userListHolder}>
                    {userList}
                </div>
            </div>
        );
    }

}
