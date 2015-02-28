var React = require('react'),
    ReactRouter = require('react-router'),
    Navigation = require('./Navigation.jsx'),
    Notifications = require('./Notifications.jsx');

var App = React.createClass({
  render: function () {
    return (
      <div>
        <Navigation brand="coreci"/>
        <div className="container">
          <ReactRouter.RouteHandler data={this.props.data}/>
        </div>
        <hr/>
        <div className="container">
          &copy; 2015 Christian Hoffmeister
        </div>
        <Notifications/>
      </div>
    );
  }
});

module.exports = App;
