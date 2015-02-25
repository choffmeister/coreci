angular.module("app").controller("buildController", ["$scope", "$timeout", "$data", "restService", ($scope, $timeout, $data, restService) ->
  $scope.buildId = $data.build.data.id
  $scope.build = $data.build.data
  $scope.output = $data.output.data

  autoRefresh($timeout, () -> restService.showBuild($scope.buildId).then (res) -> $scope.build = res.data)
  autoRefresh($timeout, () -> restService.showBuildOutput($scope.buildId).then (res) -> $scope.output = res.data)
])

angular.module("app").factory("buildController$data", ["restService", (restService) -> ($routeParams) ->
  build: restService.showBuild($routeParams.buildId)
  output: restService.showBuildOutput($routeParams.buildId)
])

autoRefresh = ($timeout, fn) ->
  fn().then(
    (res) -> $timeout((() -> autoRefresh($timeout, fn)), 2500)
  ,
    (err) -> $timeout((() -> autoRefresh($timeout, fn)), 2500)
  )

