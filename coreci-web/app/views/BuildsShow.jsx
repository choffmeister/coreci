var React = require('react'),
    Link = require('react-router').Link,
    Bluebird = require('bluebird'),
    DateTime = require('../components/DateTime.jsx'),
    Console = require('../components/Console.jsx'),
    HttpClient = require('../services/HttpClient').Raw(),
    JsonClient = require('../services/HttpClient').Json();

var BuildsShow = React.createClass({
  contextTypes: {
    router: React.PropTypes.func
  },

  statics: {
    fetchData: function (params) {
      return {
        build: JsonClient.get('/api/projects/' + params.projectCanonicalName + '/builds/' + params.buildNumber)
      };
    }
  },

  getInitialState: function () {
    return {
      build: this.props.data.build,
      output: ''
    };
  },

  componentDidMount: function () {
    this.update(this.props.data.build.projectCanonicalName, this.props.data.build.number);
    window.addEventListener('resize', this.updateConsoleHeight);
    this.updateConsoleHeight();
  },

  componentWillUnmount: function() {
    window.clearTimeout(this.outputTimeout);
    window.removeEventListener('resize', this.updateConsoleHeight);
  },

  componentDidUpdate: function () {
    this.updateConsoleHeight();
  },

  componentWillReceiveProps: function (nextProps) {
    this.setState({
      build: nextProps.data.build,
      output: ''
    });
    window.clearTimeout(this.outputTimeout);
    this.update(nextProps.data.build.projectCanonicalName, nextProps.data.build.number);
  },

  updateConsoleHeight: function () {
    var body = document.body;
    var element = this.refs.console.getDOMNode();

    var windowHeight = window.innerHeight;
    var bodyHeight = body.clientHeight;
    var elementHeight = element.clientHeight;

    element.style.height = Math.max(windowHeight - bodyHeight + elementHeight - 1, 200) + 'px';
  },

  scrollConsoleToEnd: function () {
    var element = this.refs.console.getDOMNode();
    element.scrollTop = element.scrollHeight;
  },

  rerun: function () {
    JsonClient.post('/api/builds/' + this.state.build.id + '/rerun').then(build => {
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
          <dt>Project</dt>
          <dd><Link to="projects-show" params={{projectCanonicalName: this.state.build.projectCanonicalName}}>{this.state.build.projectCanonicalName}</Link></dd>
          <dt>Image</dt>
          <dd>{this.state.build.image}</dd>
          <dt>Script</dt>
          <dd><pre>{this.state.build.script}</pre></dd>
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

  update: function (projectCanonicalName, buildNumber) {
    var build = JsonClient.get('/api/projects/' + projectCanonicalName + '/builds/' + buildNumber);
    var output = HttpClient.get('/api/projects/' + projectCanonicalName + '/builds/' + buildNumber + '/output?skip=' + this.state.output.length, true);

    Bluebird.join(build, output, (build, output) => {
      if (this.props.data.build.projectCanonicalName == projectCanonicalName && this.props.data.build.number == buildNumber) {
        this.setState({
          build: build,
          output: this.state.output + output
        });
        this.scrollConsoleToEnd();

        if (['succeeded', 'failed'].indexOf(build.status.type) < 0) {
          this.outputTimeout = window.setTimeout(function () {
            this.update(projectCanonicalName, buildNumber);
          }.bind(this), 1000);
        }
      }
    });
  }
});

module.exports = BuildsShow;
