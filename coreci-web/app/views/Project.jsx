var React = require('react'),
    ReactRouter = require('react-router'),
    DateTime = require('../components/DateTime.jsx'),
    RestClient = require('../services/RestClient');

var Project = React.createClass({
  mixins: [ReactRouter.Navigation],

  statics: {
    fetchData: function (params) {
      return {
        project: RestClient.get('/api/projects/' + params.projectCanonicalName)
      };
    }
  },

  run: function () {
    var self = this;
    RestClient.post('/api/projects/' + this.props.data['projects-show'].project.canonicalName + '/run').then(function (build) {
      self.transitionTo('builds-show', { projectCanonicalName: build.projectCanonicalName, buildNumber: build.number });
    });
  },

  render: function () {
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
          <dt>Dockerfile</dt>
          <dd><pre>{project.dockerfile}</pre></dd>
        </dl>
      </div>
    );
  }
});

module.exports = Project;
