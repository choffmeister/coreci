var UserStateStore = require('../stores/UserStateStore');

var RestClient = {};

RestClient.request = function (method, url, payload, parseJson) {
  if (parseJson === undefined) parseJson = true;

  return new Promise(function (resolve, reject) {
    var xhr = new XMLHttpRequest();
    var done = false;

    xhr.onreadystatechange = function () {
      if (xhr.readyState == 4 && !done) {
        done = true;
        switch (xhr.status) {
          case 200:
            try {
              result = parseJson ? JSON.parse(xhr.responseText) : xhr.responseText;
              resolve(result);
            } catch (ex) {
              reject(ex);
            }
            break;
          default:
            reject(new Error(xhr.statusText));
            break;
        }
      }
    };

    xhr.open(method.toUpperCase(), url, true);
    if (UserStateStore.tokenRaw) xhr.setRequestHeader('Authorization', 'Bearer ' + UserStateStore.tokenRaw);
    xhr.setRequestHeader('X-WWW-Authenticate-Filter', 'Bearer');
    xhr.send(payload);
  });
};

RestClient.get = function(url) {
  return RestClient.request('GET', url, null);
};

RestClient.post = function(url, payload) {
  return RestClient.request('POST', url, payload);
};

RestClient.put = function(url, payload) {
  return RestClient.request('PUT', url, payload);
};

RestClient.del = function(url) {
  return RestClient.request('DELETE', url);
};

module.exports = RestClient;
