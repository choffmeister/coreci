var React = require('react'),
    Link = require('react-router').Link,
    RestClient = require('../services/RestClient');

var Jobs = React.createClass({
  statics: {
    fetchData: function () {
      return {
        jobs: RestClient.get('/api/jobs')
      };
    }
  },

  render: function () {
    var jobs = this.props.data['jobs-list'].jobs.map(job => (
      <li key={job.id} className="list-group-item">
        <Link to="jobs-show" params={{jobId: job.id}}>{job.displayName}</Link>
      </li>
    ));
    return (
      <ul className="list-group">
        {jobs}
      </ul>
    );
  }
});

module.exports = Jobs;
