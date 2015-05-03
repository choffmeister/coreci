var React = require('react'),
    ReactRouter = require('react-router'),
    Route = ReactRouter.Route,
    Redirect = ReactRouter.Redirect,
    Bluebird = require('bluebird'),
    extend = require('extend');

var App = require('./components/App.jsx'),
    Projects = require('./views/Projects.jsx'),
    Project = require('./views/Project.jsx'),
    Builds = require('./views/Builds.jsx'),
    Build = require('./views/Build.jsx'),
    Workers = require('./views/Workers.jsx');

var routes = (
  <Route name="app" handler={App} path="/">
    <Route name="projects-list" handler={Projects} path="/projects"/>
    <Route name="projects-show" handler={Project} path="/projects/:projectCanonicalName"/>
    <Route name="builds-list" handler={Builds} path="/builds"/>
    <Route name="builds-show" handler={Build} path="/projects/:projectCanonicalName/builds/:buildNumber"/>
    <Route name="workers-list" handler={Workers} path="/workers"/>
    <Redirect from="" to="builds-list" />
  </Route>
);

var fetchData = function (routes, params) {
  var fetches = routes
    .filter(r => r.handler.fetchData)
    .map(r => Bluebird.props(r.handler.fetchData(params)));

  return Bluebird.reduce(fetches, (data, fetch) => extend(data, fetch), {});
};

ReactRouter.run(routes, function (Handler, state) {
  fetchData(state.routes, state.params).then((data) => {
    React.render(<Handler data={data}/>, document.body);
  });
});
