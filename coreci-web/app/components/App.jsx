var React = require('react'),
    ReactRouter = require('react-router'),
    Navigation = require('./Navigation.jsx');

var App = React.createClass({
  render: function () {
    return (
      <div>
        <Navigation brand="coreci"/>
        <div className="container">
          <ReactRouter.RouteHandler data={this.props.data}/>
        </div>
        <div className="container">
          <pre>{JSON.stringify(this.props.data.home, true, 2)}</pre>
        </div>
      </div>
    );
  }
});

module.exports = App;
