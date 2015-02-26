angular.module("app").controller("buildController", ["$scope", "$timeout", "$q", "$data", "restService", ($scope, $timeout, $q, $data, restService) ->
  $scope.buildId = $data.build.data.id
  $scope.build = $data.build.data
  $scope.output = $data.output.data

  autoRefresh $timeout, () ->
    req1 = restService.showBuild($scope.buildId)
    req2 = restService.showBuildOutput($scope.buildId)
    $q.all([req1, req2]).then (res) ->
      $scope.build = res[0].data
      $scope.output = res[1].data
])

angular.module("app").factory("buildController$data", ["restService", (restService) -> ($routeParams) ->
  build: restService.showBuild($routeParams.buildId)
  output: restService.showBuildOutput($routeParams.buildId)
])

autoRefresh = ($timeout, fn) -> fn().then () -> $timeout((() -> autoRefresh($timeout, fn)), 2500)

