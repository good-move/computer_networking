import React from 'react';

const UserListItem = ({ user }) => {
    return (
        <div className={"userListItem"}>
            <p className={"userNameView"}>{user.username}</p>
            <p className={"userStatusView"}>{user.status}</p>
        </div>
    );
};

export default UserListItem;