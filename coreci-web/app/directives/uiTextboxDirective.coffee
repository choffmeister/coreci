angular.module("app").directive("uiTextbox", ["$compile", ($compile) ->
  restrict: "E"
  replace: true
  require: "ngModel"
  scope:
    label: "@"
    placeholder: "@"
  templateUrl: "/directives/uiTextboxDirective.html"
  link: ($scope, elements, attrs) ->
    input = elements.find("input")
    input.attr("ng-model", attrs.ngModel)
    input.attr("ng-disabled", attrs.ngDisabled) if attrs.ngDisabled?
    input.attr("ui-focus-init", attrs.uiFocusInit) if attrs.uiFocusInit?
    input.attr("ui-focus-on", attrs.uiFocusOn) if attrs.uiFocusOn?
    $compile(elements)($scope.$parent)
])
