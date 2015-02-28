var React = require('react'),
    DateTime = require('../components/DateTime.jsx'),
    RestClient = require('../services/RestClient');

var Job = React.createClass({
  statics: {
    fetchData: function (params) {
      return {
        job: RestClient.get('/api/jobs/' + params.jobId)
      };
    }
  },

  render: function () {
    var job = this.props.data['jobs-show'].job;
    return (
      <div>
        <h1>Job {job.displayName}</h1>
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
