var React = require('react'),
    ReactRouter = require('react-router'),
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
  <ReactRouter.Route name="app" handler={App} path="/">
    <ReactRouter.Route name="projects-list" handler={ProjectsList} path="/projects"/>
    <ReactRouter.Route name="projects-show" handler={ProjectsShow} path="/projects/:projectCanonicalName"/>
    <ReactRouter.Route name="projects-create" handler={ProjectsCreate} path="/create"/>
    <ReactRouter.Route name="builds-list" handler={BuildsList} path="/builds"/>
    <ReactRouter.Route name="builds-show" handler={BuildsShow} path="/projects/:projectCanonicalName/builds/:buildNumber"/>
    <ReactRouter.Route name="workers-list" handler={WorkersList} path="/workers"/>
    <ReactRouter.Redirect from="" to="builds-list" />
  </ReactRouter.Route>
);

var fetchData = function (usedRoutes, params) {
  var fetches = usedRoutes
    .filter(r => r.handler.fetchData)
    .map(r => Bluebird.props(r.handler.fetchData(params)));

  return Bluebird.reduce(fetches, (data, fetch) => extend(data, fetch), {});
};

ReactRouter.run(routes, function (Handler, state) {
  fetchData(state.routes, state.params).then((data) => {
    React.render(<Handler data={data}/>, document.body);
  });
});
