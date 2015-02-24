angular.module("app").config(["$injector", "$routeProvider", "$locationProvider", ($injector, $routeProvider, $locationProvider) ->
  route = (url, templateUrl, controller, opts) ->
    options =
      templateUrl: templateUrl + ".html"
      controller: controller
      resolve:
        $data: ["$injector", "$q", "$route", ($injector, $q, $route) ->
          $routeParams = $route.current.params
          switch $injector.has("#{controller}$data")
            when true then $q.all($injector.get("#{controller}$data")($routeParams))
            else undefined
        ]
    $routeProvider.when url, options

  route("/", "/controllers/home", "homeController")

  $routeProvider.otherwise({ templateUrl: "/notfound.html" })
  $locationProvider.html5Mode(true)
])
