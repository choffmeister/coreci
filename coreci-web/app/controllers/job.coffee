angular.module("app").controller("jobController", ["$scope", "$data", ($scope, $data) ->
  $scope.job = $data.job.data
  $scope.builds = $data.builds.data
])

angular.module("app").factory("jobController$data", ["restService", (restService) -> ($routeParams) ->
  job: restService.showJob($routeParams.jobId)
  builds: restService.listBuilds($routeParams.jobId)
])
