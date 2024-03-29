var React = require('react'),
    HttpClient = require('../services/HttpClient');

var jsonClient = new HttpClient.Json();

var WorkersList = React.createClass({
  statics: {
    fetchData: function () {
      return {
        workers: jsonClient.get('/api/workers')
      };
    }
  },

  render: function () {
    var workers = this.props.data.workers.map(function (worker) {
      return (
        <tr key={worker.name}>
          <td>{worker.name}</td>
          <td>{worker.builds.length}/{worker.concurrency}</td>
          <td>{worker.dockerHostInfo ? worker.dockerHostInfo.cpus : '-'}</td>
          <td>{worker.dockerHostInfo ? Math.round(worker.dockerHostInfo.memory / 1024 / 1024) + ' MB' : '-'}</td>
          <td className="hidden-xs">{worker.dockerVersion ? worker.dockerVersion.version : '-'}</td>
        </tr>
      );
    });
    return (
      <table className="table">
        <thead>
        <tr>
          <th>worker</th>
          <th>builds</th>
          <th>cpus</th>
          <th>memory</th>
          <th className="hidden-xs">version</th>
        </tr>
        </thead>
        <tbody>
          {workers}
        </tbody>
      </table>
    );
  }
});

module.exports = WorkersList;
