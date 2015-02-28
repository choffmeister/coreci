var Reflux = require('reflux'),
    Actions = require('./Actions');

var UserStateStore = Reflux.createStore({
  user: null,

  init: function () {
    this.listenTo(Actions.Login, this.login);
    this.listenTo(Actions.Logout, this.logout);

    this.user = window.localStorage.getItem('user');
  },

  login: function(user) {
    this.user = user;
    window.localStorage.setItem('user', user);
    this.trigger(user);
  },

  logout: function () {
    this.user = null;
    window.localStorage.removeItem('user');
    this.trigger(null);
  }
});

module.exports = UserStateStore;
