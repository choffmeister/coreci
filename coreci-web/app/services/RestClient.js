var RestClient = {};

RestClient.request = function (method, url, payload) {
  return new Promise(function (resolve, reject) {
    var xhr = new XMLHttpRequest();

    xhr.onreadystatechange = function () {
      if (xhr.readyState == 4) {
        switch (xhr.status) {
          case 200:
            try {
              result = JSON.parse(xhr.responseText);
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
