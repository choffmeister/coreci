angular.module("app").service("restService", ["$http", ($http) ->
  listBuilds: () ->
    $http.get("/api/builds")
])
