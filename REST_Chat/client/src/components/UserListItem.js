import React from 'react';
import styles from '../styles/userlist.scss';

const UserListItem = ({ user }) => {
    return (
        <div className={styles.userListItem}>
            <p className={"userNameView"}>{user.username}</p>
        </div>
    );
};

export default UserListItem;