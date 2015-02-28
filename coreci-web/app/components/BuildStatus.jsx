var React = require('react');

var BuildStatus = React.createClass({
  render: function () {
    var badge;
    switch (this.props.build.status.type) {
      case 'pending':   badge = <span className="label label-default">Pending</span>; break;
      case 'running':   badge = <span className="label label-info">Running</span>; break;
      case 'succeeded': badge = <span className="label label-success">Succeeded</span>; break;
      case 'failed':    badge = <span className="label label-danger">Failed</span>; break;
      default:          badge = <span className="label label-default">Unknown</span>; break;
    }
    return badge;
  }
});

module.exports = BuildStatus;
