angular.module("app", ["ngRoute"])

angular.module("app").config(["$httpProvider", ($httpProvider) ->
  $httpProvider.interceptors.push("httpErrorLogger")
  $httpProvider.interceptors.push("authService.tokenRefresher")
  $httpProvider.interceptors.push("authService.tokenInjector")
])

angular.module("app").run(["authService", (authService) ->
  authService.initSession()
])

# close bootstrap navigation menu on route change
angular.module("app").run(["$rootScope", ($rootScope) ->
  window.setTimeout(() ->
    $rootScope.$on("$routeChangeStart", () ->
      $("button.navbar-toggle:visible:not(.collapsed)").click()
    )
  , 0)
])
