var React = require('react'),
    ReactRouter = require('react-router'),
    DateTime = require('../components/DateTime.jsx'),
    RestClient = require('../services/RestClient');

var Job = React.createClass({
  mixins: [ReactRouter.Navigation],

  statics: {
    fetchData: function (params) {
      return {
        job: RestClient.get('/api/jobs/' + params.jobId)
      };
    }
  },

  run: function () {
    var self = this;
    RestClient.post('/api/jobs/' + this.props.data['jobs-show'].job.id + '/run').then(function (build) {
      self.transitionTo('builds-show', { jobId: build.jobId, buildId: build.id });
    });
  },

  render: function () {
    var job = this.props.data['jobs-show'].job;
    return (
      <div>
        <h1>Job {job.displayName}</h1>
        <p><button onClick={this.run} className="btn btn-primary">RUN</button></p>
        <dl>
          <dt>Description</dt>
          <dd>{job.description}</dd>
          <dt>Created at</dt>
          <dd><DateTime value={job.createdAt} kind="relative"/></dd>
          <dt>Updated at</dt>
          <dd><DateTime value={job.updatedAt} kind="relative"/></dd>
          <dt>Dockerfile</dt>
          <dd><pre>{job.dockerfile}</pre></dd>
        </dl>
      </div>
    );
  }
});

module.exports = Job;
