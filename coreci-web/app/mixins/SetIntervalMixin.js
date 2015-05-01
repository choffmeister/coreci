var SetIntervalMixin = {
  componentWillMount: function() {
    this.intervals = [];
  },

  setInterval: function() {
    this.intervals.push(window.setInterval.apply(null, arguments));
  },

  componentWillUnmount: function() {
    this.intervals.map(window.clearInterval);
  }
};

module.exports = SetIntervalMixin;
