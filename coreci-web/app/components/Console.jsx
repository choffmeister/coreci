var React = require('react');

var Console = React.createClass({
  render: function () {
    return (
      <pre>{this.props.content}</pre>
    );
  }
});

module.exports = Console;
