angular.module("app").service("restService", ["$http", ($http) ->
  listBuilds: () ->
    $http.get("/api/builds")
  showBuild: (id) ->
    $http.get("/api/builds/#{id}")
  showBuildOutput: (id) ->
    $http.get("/api/builds/#{id}/output")
  createBuild: (dockerfile) ->
    $http.post("/api/builds", dockerfile)
])
