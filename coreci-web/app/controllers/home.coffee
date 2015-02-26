angular.module("app").controller("homeController", ["$scope", "$data", ($scope, $data) ->
  $scope.builds = $data.builds.data
])

angular.module("app").factory("homeController$data", ["restService", (restService) -> ($routeParams) ->
  builds: restService.listBuildsAll()
])
