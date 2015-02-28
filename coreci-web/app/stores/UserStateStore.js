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

  init: function () {
    this.listenTo(Actions.Login, this.login);
    this.listenTo(Actions.Logout, this.logout);

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
    console.log('login', username, password);
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
              this.trigger();
              Actions.Login.completed(this.username);
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
    this.trigger();
  }
});

module.exports = UserStateStore;
