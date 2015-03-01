var React = require('react');

var Console = React.createClass({
  render: function () {
    return (
      <pre className="console-output">{this.props.content}</pre>
    );
  }
});

module.exports = Console;
