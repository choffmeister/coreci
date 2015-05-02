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

  componentWillReceiveProps: function(nextProps) {
    this.setState({
      build: nextProps.data['builds-show'].build,
      output: ''
    });
    window.clearTimeout(this.outputTimeout);
    this.update(nextProps.data['builds-show'].build.projectCanonicalName, nextProps.data['builds-show'].build.number, false);
  },

  componentDidMount: function () {
    console.log(this.props.data['builds-show'].build);
    this.update(this.props.data['builds-show'].build.projectCanonicalName, this.props.data['builds-show'].build.number, false);
  },

  componentWillUnmount: function() {
    window.clearTimeout(this.outputTimeout);
  },

  rerun: function () {
    RestClient.post('/api/builds/' + this.state.build.id + '/rerun').then(build => {
      this.context.router.transitionTo('builds-show', { projectCanonicalName: build.projectCanonicalName, buildNumber: build.number });
    });
  },

  render: function () {
    var message = this.state.build.status.type == 'failed' ?
      this.state.build.status.errorMessage : '-';

    var duration = this.state.build.status.finishedAt && this.state.build.status.startedAt ?
      this.state.build.status.finishedAt - this.state.build.status.startedAt : undefined;

    return (
      <div>
        <h1>Build {this.state.build.projectCanonicalName} #{this.state.build.number}</h1>
        <p><button onClick={this.rerun} className="btn btn-primary">RERUN</button></p>
        <dl>
          <dt>Status</dt>
          <dd><span className={'build-' + this.state.build.status.type}/></dd>
          <dt>Message</dt>
          <dd><pre>{message}</pre></dd>
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

  update: function (projectCanonicalName, buildNumber, scroll) {
    var build = RestClient.get('/api/projects/' + projectCanonicalName + '/builds/' + buildNumber);
    var output = RestClient.get('/api/projects/' + projectCanonicalName + '/builds/' + buildNumber + '/output?skip=' + this.state.output.length, true);

    Promise.all2({ build: build, output: output })
      .then(res => {
        if (this.props.data['builds-show'].build.projectCanonicalName == projectCanonicalName &&
            this.props.data['builds-show'].build.number == buildNumber) {
          this.setState({
            build: res.build,
            output: this.state.output + res.output
          });

          if (scroll) {
            this.refs.console.getDOMNode().scrollIntoView(false);
          }
          if (['succeeded', 'failed'].indexOf(res.build.status.type) < 0) {
            this.outputTimeout = window.setTimeout(function () {
              this.update(projectCanonicalName, buildNumber, true);
            }.bind(this), 1000);
          }
        }
      });
  }
});

module.exports = Build;
