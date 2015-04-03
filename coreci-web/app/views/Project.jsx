var React = require('react'),
    Link = require('react-router').Link,
    RestClient = require('../services/RestClient'),
    DateTime = require('../components/DateTime.jsx');

var Project = React.createClass({
  contextTypes: {
    router: React.PropTypes.func
  },

  statics: {
    fetchData: function (params) {
      return {
        project: RestClient.get('/api/projects/' + params.projectCanonicalName),
        builds: RestClient.get('/api/projects/' + params.projectCanonicalName + '/builds')
      };
    }
  },

  run: function () {
    RestClient.post('/api/projects/' + this.props.data['projects-show'].project.canonicalName + '/run').then(build => {
      this.context.router.transitionTo('builds-show', { projectCanonicalName: build.projectCanonicalName, buildNumber: build.number });
    });
  },

  render: function () {
    var builds = this.props.data['projects-show'].builds.map(build => (
      <tr key={build.id}>
        <td className="column-icon"><span className={'build-' + build.status.type}/></td>
        <td><Link to="builds-show" params={{projectCanonicalName: build.projectCanonicalName, buildNumber: build.number}}>#{build.number}</Link></td>
        <td className="column-timestamp-relative"><DateTime value={build.updatedAt} kind="relative"/></td>
      </tr>
    ));
    var buildList = (
      <table className="table">
        <thead>
        <tr>
          <th className="column-icon"></th>
          <th>build</th>
          <th className="column-timestamp-relative"></th>
        </tr>
        </thead>
        <tbody>
          {builds}
        </tbody>
      </table>
    );

    var project = this.props.data['projects-show'].project;
    return (
      <div>
        <h1>Job {project.title}</h1>
        <p><button onClick={this.run} className="btn btn-primary">RUN</button></p>
        <dl>
          <dt>Description</dt>
          <dd>{project.description}</dd>
          <dt>Created at</dt>
          <dd><DateTime value={project.createdAt} kind="relative"/></dd>
          <dt>Updated at</dt>
          <dd><DateTime value={project.updatedAt} kind="relative"/></dd>
          <dt>Docker image</dt>
          <dd>{project.dockerRepository}</dd>
          <dt>Command</dt>
          <dd><pre>{JSON.stringify(project.command)}</pre></dd>
        </dl>
        {buildList}
      </div>
    );
  }
});

module.exports = Project;
