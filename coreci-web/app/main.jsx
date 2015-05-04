var React = require('react'),
    ReactRouter = require('react-router'),
    Route = ReactRouter.Route,
    Redirect = ReactRouter.Redirect,
    Bluebird = require('bluebird'),
    extend = require('extend');

var App = require('./components/App.jsx'),
    ProjectsList = require('./views/ProjectsList.jsx'),
    ProjectsShow = require('./views/ProjectsShow.jsx'),
    ProjectsCreate = require('./views/ProjectsCreate.jsx'),
    BuildsList = require('./views/BuildsList.jsx'),
    BuildsShow = require('./views/BuildsShow.jsx'),
    WorkersList = require('./views/WorkersList.jsx');

var routes = (
  <Route name="app" handler={App} path="/">
    <Route name="projects-list" handler={ProjectsList} path="/projects"/>
    <Route name="projects-show" handler={ProjectsShow} path="/projects/:projectCanonicalName"/>
    <Route name="projects-create" handler={ProjectsCreate} path="/create"/>
    <Route name="builds-list" handler={BuildsList} path="/builds"/>
    <Route name="builds-show" handler={BuildsShow} path="/projects/:projectCanonicalName/builds/:buildNumber"/>
    <Route name="workers-list" handler={WorkersList} path="/workers"/>
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
