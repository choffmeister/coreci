var React = require('react'),
    RestClient = require('../services/RestClient'),
    DateTime = require('../components/DateTime.jsx'),
    Console = require('../components/Console.jsx'),
    BuildStatus = require('../components/BuildStatus.jsx');

var Build = React.createClass({
  statics: {
    fetchData: function (params) {
      return {
        build: RestClient.get('/api/jobs/' + params.jobId + '/builds/' + params.buildId),
        output: RestClient.request('GET', '/api/jobs/' + params.jobId + '/builds/' + params.buildId + '/output', null, false)
      };
    }
  },

  render: function () {
    var build = this.props.data['builds-show'].build;
    var output = this.props.data['builds-show'].output;

    var console;
    if (output) {
      console = <Console content={output}/>;
    } else {
      console = <span>No console output</span>;
    }

    return (
      <div>
        <h1>Build {build.id}</h1>
        <dl>
          <dt>Status</dt>
          <dd><BuildStatus build={build}/></dd>
          <dt>Created at</dt>
          <dd><DateTime value={build.createdAt} kind="relative"/></dd>
          <dt>Updated at</dt>
          <dd><DateTime value={build.updatedAt} kind="relative"/></dd>
          <dt>Started at</dt>
          <dd><DateTime value={build.status.startedAt} kind="relative"/></dd>
          <dt>Finished at</dt>
          <dd><DateTime value={build.status.finishedAt} kind="relative"/></dd>
          <dt>Output</dt>
          <dd>{console}</dd>
        </dl>
      </div>
    );
  }
});

module.exports = Build;
