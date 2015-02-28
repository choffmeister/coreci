var React = require('react'),
    ReactRouter = require('react-router'),
    Route = ReactRouter.Route,
    DefaultRoute = ReactRouter.DefaultRoute,
    RestClient = require('./services/RestClient');

var App = require('./components/App.jsx'),
    Home = require('./components/Home.jsx'),
    About = require('./components/About.jsx');

var routes = (
  <Route name="app" handler={App} path="/">
    <DefaultRoute handler={Home}/>
    <Route name="home" handler={Home} path="/"/>
    <Route name="about" handler={About}/>
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
