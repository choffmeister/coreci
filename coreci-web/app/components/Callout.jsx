var React = require('react');

var Callout = React.createClass({
  propTypes: {
    kind: React.PropTypes.oneOf([
      'error',
      'warning',
      'success',
      'info'
    ]).isRequired
  },

  render: function () {
    switch (this.props.kind) {
      case 'error':
        return <div className="alert alert-danger" role="alert" {...this.props}>{this.props.children}</div>;
      case 'warning':
        return <div className="alert alert-warning" role="alert" {...this.props}>{this.props.children}</div>;
      case 'success':
        return <div className="alert alert-success" role="alert" {...this.props}>{this.props.children}</div>;
      case 'info':
        return <div className="alert alert-info" role="alert" {...this.props}>{this.props.children}</div>;
    }
    return <div>{this.props.kind}</div>;
  }
});

module.exports = Callout;
