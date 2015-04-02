var parseBase64UrlSafe = function (b64) {
  return atob(b64.replace(/\-/g, '+').replace(/_/g, '/'));
};

var parseAccessToken = function (tokenStr) {
  return JSON.parse(parseBase64UrlSafe(tokenStr.split('.')[1]));
};

var obtainAccessToken = function (url, authorization) {
  var promise = new Promise(function(resolve, reject) {
    var xhr = new XMLHttpRequest();
    xhr.onreadystatechange = function () {
      if (xhr.readyState == 4) {
        switch (xhr.status) {
          case 200:
            try {
              var response = JSON.parse(xhr.responseText);
              if (response.token_type == "bearer" && response.access_token) {
                resolve(response.access_token);
              } else {
                reject(new Error("Unsupported token response"));
              }
            } catch (ex) {
              reject(ex);
            }
            break;
          case 404:
          case 401:
            resolve(null);
            break;
          default:
            reject(new Error("An unknown error occured"));
            break;
        }
      }
    };

    xhr.open('GET', url, true);
    xhr.setRequestHeader('Authorization', authorization);
    xhr.setRequestHeader('X-WWW-Authenticate-Filter', 'Bearer');
    xhr.send();
  });

  return promise.then(function (at) {
    window.localStorage.setItem('access_token', at);
    return at;
  });
};

var AccessToken = {};

AccessToken.create = function (username, password) {
  return obtainAccessToken('/api/auth/token/create', 'Basic ' + btoa(username + ':' + password));
};

AccessToken.renew = function () {
  return obtainAccessToken('/api/auth/token/renew', 'Bearer ' + AccessToken.current());
};

AccessToken.current = function () {
  return window.localStorage.getItem('access_token');
};

AccessToken.remove = function () {
  window.localStorage.removeItem('access_token');
};

module.exports = AccessToken;
