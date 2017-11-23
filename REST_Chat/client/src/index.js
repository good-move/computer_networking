import React from 'react';
import ReactDOM from 'react-dom';
import { BrowserRouter, Switch, Route } from 'react-router-dom';

import styles from './styles/main.scss';

import LoginPage from './pages/login.js';
import ChatPage from './pages/chat.js';
import NotFoundPage from './pages/not_found.js';


const App = () => {
  return (
      <Switch>
          <Route exact path='/' component={LoginPage} />
          <Route path='/login' component={LoginPage} />
          <Route path='/chat' component={ChatPage} />
          <Route component={NotFoundPage} />
      </Switch>
  );
};


ReactDOM.render(
    <BrowserRouter>
        <App />
    </BrowserRouter>,
    document.getElementById('app')
);
