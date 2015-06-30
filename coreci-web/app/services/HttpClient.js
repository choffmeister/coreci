/*eslint dot-notation: 0*/
/*eslint no-alert: 0*/
var Bluebird = require('bluebird'),
    extend = require('extend'),
    AccessToken = require('./AccessToken');

var rawRequest = function (method, url, body, options) {
  options = extend({
    headers: {}
  }, options);

  return new Promise(function (resolve, reject) {
    var xhr = new XMLHttpRequest();

    xhr.onreadystatechange = function () {
      if (xhr.readyState === 4) {
        if (xhr.status >= 200 && xhr.status < 300) {
          resolve({
            status: xhr.status,
            message: xhr.statusText,
            body: xhr.responseText,
            xhr: xhr
          });
        } else if (xhr.status === 0) {
          reject({
            type: 'socket_error',
            xhr: xhr
          });
        } else {
          reject({
            type: 'http_not_ok',
            status: xhr.status,
            message: xhr.statusText,
            body: xhr.responseText,
            xhr: xhr
          });
        }
      }
    };

    xhr.open(method, url, true);
    Object.keys(options.headers).forEach(function (header) {
      xhr.setRequestHeader(header, options.headers[header]);
    });
    xhr.send(body);
  });
};

var HttpClient = function (request) {
  var transformRes = (res) => res.body;

  return {
    get: function (url, options) {
      return request('GET', url, null, options).then(transformRes);
    },
    post: function (url, body, options) {
      return request('POST', url, body, options).then(transformRes);
    },
    put: function (url, body, options) {
      return request('PUT', url, body, options).then(transformRes);
    },
    del: function (url, options) {
      return request('DELETE', url, null, options).then(transformRes);
    }
  };
};

var JsonHttpClient = function (request) {
  var transformReqBody = (reqBody) => reqBody ? JSON.stringify(reqBody) : undefined;
  var transformRes = (res) => res.body ? JSON.parse(res.body) : undefined;
  var extendReqHeaders = function (options) {
    return extend(true, options, {
      headers: {
        'Accept': 'application/json',
        'Content-Type': 'application/json'
      }
    });
  };

  return {
    get: function (url, options) {
      return request('GET', url, null, extendReqHeaders(options)).then(transformRes);
    },
    post: function (url, body, options) {
      return request('POST', url, transformReqBody(body), extendReqHeaders(options)).then(transformRes);
    },
    put: function (url, body, options) {
      return request('PUT', url, transformReqBody(body), extendReqHeaders(options)).then(transformRes);
    },
    del: function (url, options) {
      return request('DELETE', url, null, extendReqHeaders(options)).then(transformRes);
    }
  };
};

var withAuthentication = function (request, autoRenew) {
  var hasTokenExpired = function (err) {
    if (err.status === 401) {
      var authHeader = err.xhr.getResponseHeader('www-authenticate');
      if (authHeader && authHeader.toLowerCase().substring(0, 7) === 'bearer ' && authHeader.indexOf('expired') > 0) {
        return true;
      }
    }
    return false;
  };

  return function (method, url, body, options) {
    options = extend({
      headers: {}
    }, options);

    options.headers['X-WWW-Authenticate-Filter'] = 'Bearer';
    if (AccessToken.current()) {
      options.headers['Authorization'] = 'Bearer ' + AccessToken.current();
    }

    return request(method, url, body, options)
      .catch(function (err) {
        if (hasTokenExpired(err) && autoRenew) {
          return AccessToken.renew().then(function (token) {
            return token ? withAuthentication(request, false)(method, url, body, options) : Bluebird.reject(err);
          });
        } else {
          throw err;
        }
      });
  };
};

var withGlobalErrorAlert = function (request) {
  return function (method, url, body, options) {
    return request(method, url, body, options)
      .catch(function (err) {
        delete err.xhr;
        window.alert(JSON.stringify(err, true, 2));
        throw err;
      });
  };
};

module.exports = {
  Raw: function () {
    return new HttpClient(withGlobalErrorAlert(withAuthentication(rawRequest, true)));
  },
  Json: function () {
    return new JsonHttpClient(withGlobalErrorAlert(withAuthentication(rawRequest, true)));
  }
};
