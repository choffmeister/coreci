var Reflux = require('reflux');

Actions = {
  Login: Reflux.createAction({ asyncResult: true }),
  Logout: Reflux.createAction(),
  NotificationAdd: Reflux.createAction(),
  NotificationDrop: Reflux.createAction()
};

module.exports = Actions;
