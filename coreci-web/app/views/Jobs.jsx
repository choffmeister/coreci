var React = require('react'),
    Link = require('react-router').Link,
    RestClient = require('../services/RestClient'),
    DateTime = require('../components/DateTime.jsx');

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
      <tr key={job.id}>
        <td><Link to="jobs-show" params={{jobId: job.id}}>{job.displayName}</Link></td>
        <td className="column-timestamp-relative"><DateTime value={job.updatedAt} kind="relative"/></td>
      </tr>
    ));
    return (
      <table className="table">
        <thead>
          <tr>
            <th>job</th>
            <th className="column-timestamp-relative"></th>
          </tr>
        </thead>
        <tbody>
          {jobs}
        </tbody>
      </table>
    );
  }
});

module.exports = Jobs;
