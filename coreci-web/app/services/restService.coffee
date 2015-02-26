angular.module("app").service("restService", ["$http", ($http) ->
  listJobs: () ->
    $http.get("/api/jobs")
  showJob: (jobId) ->
    $http.get("/api/jobs/#{jobId}")
  runJob: (jobId) ->
    $http.post("/api/jobs/#{jobId}/run")
  listBuildsAll: () ->
    $http.get("/api/builds")
  listBuilds: (jobId) ->
    $http.get("/api/jobs/#{jobId}/builds")
  showBuild: (jobId, buildId) ->
    $http.get("/api/jobs/#{jobId}/builds/#{buildId}")
  showBuildOutput: (jobId, buildId) ->
    $http.get("/api/jobs/#{jobId}/builds/#{buildId}/output")
])
