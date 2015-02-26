angular.module("app").controller("jobController", ["$scope", "$location", "$data", "restService", ($scope, $location, $data, restService) ->
  $scope.job = $data.job.data
  $scope.builds = $data.builds.data

  $scope.run = () ->
    restService.runJob($scope.job.id).then (res) ->
      build = res.data
      $location.path("/jobs/#{build.jobId}/builds/#{build.id}")
])

angular.module("app").factory("jobController$data", ["restService", (restService) -> ($routeParams) ->
  job: restService.showJob($routeParams.jobId)
  builds: restService.listBuilds($routeParams.jobId)
])
