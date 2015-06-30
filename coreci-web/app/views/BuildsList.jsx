var React = require('react'),
    Link = require('react-router').Link,
    DateTime = require('../components/DateTime.jsx'),
    HttpClient = require('../services/HttpClient');

var jsonClient = new HttpClient.Json();

var BuildsList = React.createClass({
  statics: {
    fetchData: function () {
      return {
        builds: jsonClient.get('/api/builds')
      };
    }
  },

  render: function () {
    var builds = this.props.data.builds.map(build => {
      var duration = build.status.finishedAt && build.status.startedAt ?
        build.status.finishedAt - build.status.startedAt : undefined;

      return (
        <tr key={build.id}>
          <td className="column-icon"><span className={'build-' + build.status.type}/></td>
          <td>
            <Link to="projects-show" params={{projectCanonicalName: build.projectCanonicalName}}>{build.projectCanonicalName}</Link>&nbsp;
            <Link to="builds-show" params={{projectCanonicalName: build.projectCanonicalName, buildNumber: build.number}}>#{build.number}</Link>
          </td>
          <td className="column-timestamp-relative hidden-xs">
            <span className="glyphicon glyphicon-calendar"/>&nbsp;
            <DateTime value={build.updatedAt} kind="relative"/>
          </td>
          <td className="column-runtime-duration hidden-xs">
            <span className="glyphicon glyphicon-time"/>&nbsp;
            <DateTime value={duration} kind="duration"/>
          </td>
        </tr>
      );
    });
    var buildList = (
      <table className="table">
        <thead>
          <tr>
            <th className="column-icon"></th>
            <th>build</th>
            <th className="column-timestamp-relative hidden-xs"></th>
            <th className="column-runtime-duration hidden-xs"></th>
          </tr>
        </thead>
        <tbody>
          {builds}
        </tbody>
      </table>
    );

    return (
      <div>
        {buildList}
      </div>
    );
  }
});

module.exports = BuildsList;
