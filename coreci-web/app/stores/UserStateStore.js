var Reflux = require('reflux'),
    Actions = require('./Actions');

var parseBase64UrlSafe = function (b64) {
      return atob(b64.replace(/\-/g, '+').replace(/_/g, '/'));
    },
    parseTokenRaw = function (tokenStr) {
      return JSON.parse(parseBase64UrlSafe(tokenStr.split('.')[1]));
    };

var UserStateStore = Reflux.createStore({
  tokenRaw: null,
  token: null,
  username: null,
  notifications: [],
  nextNotificationId: 0,

  init: function () {
    this.listenTo(Actions.Login, this.login);
    this.listenTo(Actions.Logout, this.logout);
    this.listenTo(Actions.NotificationAdd, this.notificationAdd);
    this.listenTo(Actions.NotificationDrop, this.notificationDrop);

    if (window.localStorage.getItem('token')) {
      try {
        this.setToken(window.localStorage.getItem('token'));
      } catch (ex) {
        this.unsetToken();
        console.error(ex);
      }
    }
  },

  setToken: function (tokenRaw) {
    this.token = parseTokenRaw(tokenRaw);
    this.tokenRaw = tokenRaw;
    this.username = this.token.name;
    window.localStorage.setItem('token', tokenRaw);
  },

  unsetToken: function () {
    window.localStorage.removeItem('token');
    this.username = null;
    this.token = null;
    this.tokenRaw = null;
  },

  login: function(username, password) {
    var xhr = new XMLHttpRequest();
    var done = false;

    xhr.onreadystatechange = function () {
      if (xhr.readyState == 4 && !done) {
        done = true;
        switch (xhr.status) {
          case 200:
            try {
              var response = JSON.parse(xhr.responseText);
              this.setToken(response.access_token);
              this.notifications = [];
              this.trigger();
              Actions.Login.completed(this.username);
              Actions.NotificationAdd('Welcome, ' + this.username + '!');
            } catch (ex) {
              Actions.Login.failed(ex);
            }
            break;
          case 401:
          case 404:
            Actions.Login.completed(null);
            break;
          default:
            Actions.Login.failed(new Error(xhr.statusText));
            break;
        }
      }
    }.bind(this);

    xhr.open('GET', '/api/auth/token/create', true);
    xhr.setRequestHeader('Authorization', 'Basic ' + btoa(username + ':' + password));
    xhr.setRequestHeader('X-WWW-Authenticate-Filter', 'Bearer');
    xhr.send();
  },

  logout: function () {
    this.unsetToken();
    this.notifications = [];
    this.trigger();
    Actions.NotificationAdd('Goodbye!');
  },

  notificationAdd: function (text) {
    this.notifications.push({
      id: this.nextNotificationId++,
      text: text
    });
    this.trigger();
  },

  notificationDrop: function (id) {
    for (var i = 0, l = this.notifications.length; i < l; i++) {
      if (this.notifications[i].id == id) {
        this.notifications.splice(i, 1);
        this.trigger();
        return;
      }
    }
  }
});

module.exports = UserStateStore;
