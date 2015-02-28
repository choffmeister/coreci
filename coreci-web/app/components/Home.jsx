var React = require('react'),
    Link = require('react-router').Link,
    RestClient = require('../services/RestClient');

var Home = React.createClass({
  statics: {
    fetchData: function () {
      return {
        builds: RestClient.get('/api/builds')
      };
    }
  },

  render: function () {
    var builds = this.props.data.home.builds.map(b => (
      <div key={b.id}>
        <Link to="home">{b.id}</Link>
      </div>
    ));
    return (
      <div>
        {builds}
      </div>
    );
  }
});

module.exports = Home;
