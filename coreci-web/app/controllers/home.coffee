angular.module("app").controller("homeController", ["$scope", "$data", ($scope, $data) ->
  $scope.builds = $data.builds
])

angular.module("app").factory("homeController$data", ["restService", (restService) -> ($routeParams) ->
  builds: restService.listBuilds()
])
