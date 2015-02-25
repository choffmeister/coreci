angular.module("app").directive("uiFocusInit", ["$timeout", ($timeout) ->
  focus = ($timeout, element) -> $timeout((() -> element[0].focus()), 0)

  link: ($scope, element) ->
    focus($timeout, element)
])
