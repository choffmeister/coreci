var React = require('react'),
    Reflux = require('reflux'),
    Actions = require('../stores/Actions'),
    UserStateStore = require('../stores/UserStateStore');

var Notification = React.createClass({
  onClick: function () {
    Actions.NotificationDrop(this.props.id);
  },

  render: function () {
    return (
      <div>
        <div onClick={this.onClick} className="notification">
          {this.props.text}
        </div>
      </div>
    );
  }
});

var Notifications = React.createClass({
  mixins: [Reflux.ListenerMixin],

  componentWillMount : function () {
    this.onUserStateChanged();
  },

  componentDidMount: function () {
    this.listenTo(UserStateStore, this.onUserStateChanged);
  },

  onUserStateChanged: function () {
    this.setState({
      notifications: UserStateStore.notifications
    });
  },

  render: function () {
    var messages = this.state.notifications.map(notification => (
      <Notification key={notification.id} id={notification.id} text={notification.text}/>
    ));
    return (
      <div id="notifications">
        {messages}
      </div>
    );
  }
});

module.exports = Notifications;
