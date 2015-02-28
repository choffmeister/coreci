var React = require('react'),
    moment = require('moment'),
    SetIntervalMixin = require('../mixins/SetIntervalMixin');

var DateTime = React.createClass({
  mixins: [SetIntervalMixin],

  propTypes: {
    value: React.PropTypes.oneOfType([
      React.PropTypes.number,
      React.PropTypes.instanceOf(Date)
    ]),
    kind: React.PropTypes.oneOf([
      'relative'
    ]).isRequired
  },

  componentDidMount: function () {
    this.setInterval(this.forceUpdate.bind(this), 1000);
  },

  render: function () {
    switch (this.props.kind) {
      case 'relative':
        if (this.props.value) {
          var mom = moment(new Date(this.props.value));

          return (
            <span title={mom.format('YYYY-MM-DD hh:mm:ss.SSS')} {...this.props}>{mom.fromNow()}</span>
          );
        } else {
          return (
            <span {...this.props}>-</span>
          );
        }
        break;
    }
  }
});

module.exports = DateTime;
