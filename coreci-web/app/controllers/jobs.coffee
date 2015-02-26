angular.module("app").controller("jobsController", ["$scope", "$data", ($scope, $data) ->
  $scope.jobs = $data.jobs.data
])

angular.module("app").factory("jobsController$data", ["restService", (restService) -> ($routeParams) ->
  jobs: restService.listJobs()
])
