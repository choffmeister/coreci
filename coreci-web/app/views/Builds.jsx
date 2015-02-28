var React = require('react'),
    Link = require('react-router').Link,
    RestClient = require('../services/RestClient'),
    DateTime = require('../components/DateTime.jsx'),
    BuildStatus = require('../components/BuildStatus.jsx');

var Builds = React.createClass({
  statics: {
    fetchData: function () {
      return {
        builds: RestClient.get('/api/builds')
      };
    }
  },

  render: function () {
    var builds = this.props.data['builds-list'].builds.map(build => (
      <li key={build.id} className="list-group-item">
        <BuildStatus build={build}/>
        <Link to="builds-show" params={{jobId: build.jobId, buildId: build.id}}>{build.id}</Link>
        <span className="pull-right">
          <DateTime value={build.updatedAt} kind="relative"/>
        </span>
      </li>
    ));
    return (
      <ul className="list-group">
        {builds}
      </ul>
    );
  }
});

module.exports = Builds;
