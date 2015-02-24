angular.module("app").controller("buildController", ["$scope", "$data", ($scope, $data) ->
  $scope.build = $data.build.data
  $scope.output = $data.output.data
])

angular.module("app").factory("buildController$data", ["restService", (restService) -> ($routeParams) ->
  build: restService.showBuild($routeParams.buildId)
  output: restService.showBuildOutput($routeParams.buildId)
])
