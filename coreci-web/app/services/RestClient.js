var AccessToken = require('./AccessToken');

var RestClient = {};

var request = function (method, url, payload, raw, depth) {
  if (depth === undefined) depth = 0;

  return new Promise(function (resolve, reject) {
    var xhr = new XMLHttpRequest();
    xhr.onreadystatechange = function () {
      if (xhr.readyState == 4) {
        switch (xhr.status) {
          case 200:
            try {
              result = raw === true ? xhr.responseText : JSON.parse(xhr.responseText);
              resolve(result);
            } catch (ex) {
              reject(ex);
            }
            break;
          case 401:
            var authHeader = xhr.getResponseHeader('www-authenticate');
            if (authHeader && authHeader.toLowerCase().substring(0, 7) == 'bearer ' && authHeader.indexOf('expired') > 0 && depth < 1) {
              AccessToken.renew()
                .then(token => request(method, url, payload, raw, depth + 1).then(res => resolve(res)).catch(err => reject(err)))
                .catch(err => reject(err))
            } else {
              reject(new Error(xhr.statusText));
            }
            break;
          default:
            reject(new Error(xhr.statusText));
            break;
        }
      }
    };

    xhr.open(method, url, true);
    if (AccessToken.current()) xhr.setRequestHeader('Authorization', 'Bearer ' + AccessToken.current());
    xhr.setRequestHeader('X-WWW-Authenticate-Filter', 'Bearer');
    xhr.send(payload);
  });
};

RestClient.get = function(url, raw) {
  return request('GET', url, null, raw);
};

RestClient.post = function(url, payload, raw) {
  return request('POST', url, payload, raw);
};

RestClient.put = function(url, payload, raw) {
  return request('PUT', url, payload, raw);
};

RestClient.del = function(url, raw) {
  return request('DELETE', url, raw);
};

module.exports = RestClient;
