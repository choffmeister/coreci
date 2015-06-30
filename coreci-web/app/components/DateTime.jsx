var React = require('react'),
    moment = require('moment'),
    SetIntervalMixin = require('../mixins/SetIntervalMixin');

// enable format extension
require('moment-duration-format');

var DateTime = React.createClass({
  mixins: [SetIntervalMixin],

  propTypes: {
    value: React.PropTypes.oneOfType([
      React.PropTypes.number,
      React.PropTypes.instanceOf(Date)
    ]),
    kind: React.PropTypes.oneOf([
      'relative',
      'duration'
    ]).isRequired
  },

  componentDidMount: function () {
    this.setInterval(this.forceUpdate.bind(this), 1000);
  },

  render: function () {
    switch (this.props.kind) {
      case 'relative':
        if (this.props.value !== undefined && this.props.value !== null) {
          var mom1 = moment(new Date(this.props.value));

          return (
            <span title={mom1.format('YYYY-MM-DD hh:mm:ss.SSS')} {...this.props}>{mom1.fromNow()}</span>
          );
        } else {
          return (
            <span {...this.props}>-</span>
          );
        }
        break;
      case 'duration':
        if (this.props.value !== undefined && this.props.value !== null) {
          var time = new Date(this.props.value).getTime();
          var mom2 = moment.duration(time);
          var format = time >= 60000 ? 'm [min] s [sec]' : 's [sec] S [ms]';

          return (
            <span {...this.props}>{mom2.format(format)}</span>
          );
        } else {
          return (
            <span {...this.props}>-</span>
          );
        }
        break;
      default:
        throw new Error('Unkown date time kind ' + this.props.kind);
    }
  }
});

module.exports = DateTime;
