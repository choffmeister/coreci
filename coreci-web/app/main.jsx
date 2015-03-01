var React = require('react'),
    ReactRouter = require('react-router'),
    Route = ReactRouter.Route,
    Redirect = ReactRouter.Redirect;

var App = require('./components/App.jsx'),
    Jobs = require('./views/Jobs.jsx'),
    Job = require('./views/Job.jsx'),
    Builds = require('./views/Builds.jsx'),
    Build = require('./views/Build.jsx');

var routes = (
  <Route name="app" handler={App} path="/">
    <Route name="jobs-list" handler={Jobs} path="/jobs"/>
    <Route name="jobs-show" handler={Job} path="/jobs/:jobId"/>
    <Route name="builds-list" handler={Builds} path="/builds"/>
    <Route name="builds-show" handler={Build} path="/jobs/:jobId/builds/:buildId"/>
    <Redirect from="" to="builds-list" />
  </Route>
);

Promise.all2 = function(f) {
  if (f instanceof Promise) {
    return f;
  } else if (Array.isArray(f)) {
    return Promise.all(f);
  } else if (typeof f == 'object') {
    var keys = [];
    var valueFutures = [];
    for (key in f) {
      if (f.hasOwnProperty(key)) {
        keys.push(key);
        valueFutures.push(f[key]);
      }
    }
    return Promise.all(valueFutures).then(values => {
      var result = {};
      for (var i = 0, l = keys.length; i < l; i++) {
        result[keys[i]] = values[i];
      }
      return result;
    });
  } else {
    throw new Error('Not supported');
  }
};

var fetchData = function (routes, params) {
  var data = {};
  var fetch = routes
    .filter(r => r.handler.fetchData)
    .map(r => Promise.all2(r.handler.fetchData(params)).then(d => data[r.name] = d));

  return Promise.all(fetch).then(() => data);
};

ReactRouter.run(routes, function (Handler, state) {
  fetchData(state.routes, state.params).then((data) => {
    React.render(<Handler data={data}/>, document.body);
  });
});
