var React = require('react'),
  RestClient = require('../services/RestClient');

var Workers = React.createClass({
  statics: {
    fetchData: function () {
      return {
        workers: RestClient.get('/api/workers')
      };
    }
  },

  render: function () {
    console.log(this.props.data['workers-list'].workers);
    var workerMap = this.props.data['workers-list'].workers;
    var workers = Object.keys(workerMap).map(function (uri) {
      var worker = workerMap[uri];
      return (
        <tr key={uri}>
          <td>{uri}</td>
          <td>{worker.builds.length}/{worker.concurrency}</td>
          <td>{worker.hostInfo ? worker.hostInfo.cpus : '-'}</td>
          <td>{worker.hostInfo ? Math.round(worker.hostInfo.memory / 1024 / 1024) + ' MB' : '-'}</td>
          <td className="hidden-xs">{worker.version ? worker.version.version : '-'}</td>
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

module.exports = Workers;
