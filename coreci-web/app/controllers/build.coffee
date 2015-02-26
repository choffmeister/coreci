angular.module("app").controller("buildController", ["$scope", "$timeout", "$q", "$data", "$routeParams", "restService", ($scope, $timeout, $q, $data, $routeParams, restService) ->
  $scope.jobId = $routeParams.jobId
  $scope.buildId = $routeParams.buildId
  $scope.build = $data.build.data
  $scope.output = $data.output.data

  autoRefresh $timeout, () ->
    req1 = restService.showBuild($scope.jobId, $scope.buildId)
    req2 = restService.showBuildOutput($scope.jobId, $scope.buildId)
    $q.all([req1, req2]).then (res) ->
      $scope.build = res[0].data
      $scope.output = res[1].data
])

angular.module("app").factory("buildController$data", ["restService", (restService) -> ($routeParams) ->
  build: restService.showBuild($routeParams.jobId, $routeParams.buildId)
  output: restService.showBuildOutput($routeParams.jobId, $routeParams.buildId)
])

autoRefresh = ($timeout, fn) -> fn().then () -> $timeout((() -> autoRefresh($timeout, fn)), 2500)

