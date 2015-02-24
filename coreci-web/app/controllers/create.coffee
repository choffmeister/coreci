angular.module("app").controller("createController", ["$scope", "$location", "restService", ($scope, $location, restService) ->
  $scope.dockerfile = ""

  $scope.run = () ->
    restService.createBuild($scope.dockerfile).then (build) ->
      $location.path("/builds/#{build.data.id}").replace(true)
])
