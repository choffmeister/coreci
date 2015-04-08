var React = require('react'),
    RestClient = require('../services/RestClient'),
    DateTime = require('../components/DateTime.jsx'),
    Console = require('../components/Console.jsx');

var Build = React.createClass({
  contextTypes: {
    router: React.PropTypes.func
  },

  statics: {
    fetchData: function (params) {
      return {
        build: RestClient.get('/api/projects/' + params.projectCanonicalName + '/builds/' + params.buildNumber)
      };
    }
  },

  getInitialState: function () {
    return {
      build: this.props.data['builds-show'].build,
      output: ''
    };
  },

  componentDidMount: function () {
    this.updateConsoleOutput(false);
  },

  componentWillUnmount: function() {
    window.clearTimeout(this.outputTimeout);
  },

  render: function () {
    var error = this.state.build.status.type == 'failed' ?
      <pre>{"ERROR\n\n" + this.state.build.status.errorMessage}</pre> : undefined;

    var duration = this.state.build.status.finishedAt && this.state.build.status.startedAt ?
      this.state.build.status.finishedAt - this.state.build.status.startedAt : undefined;

    return (
      <div>
        <h1>Build {this.state.build.projectCanonicalName} #{this.state.build.number}</h1>
        {error}
        <dl>
          <dt>Status</dt>
          <dd><span className={'build-' + this.state.build.status.type}/></dd>
          <dt>Created at</dt>
          <dd><DateTime value={this.state.build.createdAt} kind="relative"/></dd>
          <dt>Updated at</dt>
          <dd><DateTime value={this.state.build.updatedAt} kind="relative"/></dd>
          <dt>Started at</dt>
          <dd><DateTime value={this.state.build.status.startedAt} kind="relative"/></dd>
          <dt>Finished at</dt>
          <dd>
            <span className="glyphicon glyphicon-calendar"/>&nbsp;
            <DateTime value={this.state.build.status.finishedAt} kind="relative"/>
          </dd>
          <dt>Duration</dt>
          <dd>
            <span className="glyphicon glyphicon-time"/>&nbsp;
            <DateTime value={duration} kind="duration"/>
          </dd>
          <dt>Output</dt>
          <dd>
            <Console content={this.state.output} ref="console"/>
          </dd>
        </dl>
      </div>
    );
  },

  updateConsoleOutput: function (scroll) {
    var params = this.context.router.getCurrentParams();
    var build = RestClient.get('/api/projects/' + params.projectCanonicalName + '/builds/' + params.buildNumber);
    var output = RestClient.get('/api/projects/' + params.projectCanonicalName + '/builds/' + params.buildNumber + '/output?skip=' + this.state.output.length, true);

    Promise.all2({ build: build, output: output })
      .then(res => {
        this.setState({
          build: res.build,
          output: this.state.output + res.output
        });

        if (scroll) {
          this.refs.console.getDOMNode().scrollIntoView(false);
        }
        if (['succeeded', 'failed'].indexOf(res.build.status.type) < 0) {
          this.outputTimeout = window.setTimeout(function () {
            this.updateConsoleOutput(true);
          }.bind(this), 1000);
        }
      });
  }
});

module.exports = Build;
