angular.module("app").factory("httpErrorLogger", ["$q", "flashService", ($q, flashService) ->
  responseError: (res) ->
    if not res.config.preventErrorLogging is true
      message = switch res.status
        when 0 then "Could not connect to server!"
        when 401 then "HTTP 401 Unauthorized"
        when 403 then "HTTP 403 Forbidden"
        when 404 then "HTTP 404 Not found!"
        when 500 then "HTTP 500 Internal server error!"
        else "An unknown HTTP error occured!"
      flashService.error(message)
    $q.reject(res)
])
