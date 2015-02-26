angular.module("app").controller("buildController", ["$scope", "$timeout", "$q", "$data", "$routeParams", "restService", ($scope, $timeout, $q, $data, $routeParams, restService) ->
  $scope.jobId = $routeParams.jobId
  $scope.buildId = $routeParams.buildId
  $scope.build = $data.build.data
  $scope.output = $data.output.data

  autorefresh = null
  refresh = () ->
    req1 = restService.showBuild($scope.jobId, $scope.buildId)
    req2 = restService.showBuildOutput($scope.jobId, $scope.buildId)
    $q.all([req1, req2]).then (res) ->
      $scope.build = res[0].data
      $scope.output = res[1].data
      autorefresh = $timeout(refresh, 2500)
  autorefresh = $timeout(refresh, 2500)

  $scope.$on "$destroy", () -> $timeout.cancel(autorefresh)
])

angular.module("app").factory("buildController$data", ["restService", (restService) -> ($routeParams) ->
  build: restService.showBuild($routeParams.jobId, $routeParams.buildId)
  output: restService.showBuildOutput($routeParams.jobId, $routeParams.buildId)
])
