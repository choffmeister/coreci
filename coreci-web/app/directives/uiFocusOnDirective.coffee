angular.module("app").directive("uiFocusOn", ["$timeout", ($timeout) ->
  focus = ($timeout, element) -> $timeout((() -> element[0].focus()), 0)

  link: ($scope, element, attrs) ->
    $scope.$on attrs.uiFocusOn, () ->
      focus($timeout, element)
])
