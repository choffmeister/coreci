var Reflux = require('reflux');

Actions = {
  Login: Reflux.createAction({ asyncResult: true }),
  Logout: Reflux.createAction()
};

module.exports = Actions;
