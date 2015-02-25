angular.module("app").service("authService", ["$http", "$rootScope", "storageService", "flashService", ($http, $rootScope, storageService, flashService) ->
  parseBase64UrlSafe = (b64) ->
    atob(b64.replace(/\-/g, "+").replace(/_/g, "/"))
  parseToken = (tokenStr) ->
    JSON.parse(parseBase64UrlSafe(tokenStr.split(".")[1]))

  isAuthenticated: () ->
    storageService.get("session")?.isAuthenticated or false
  getBearerToken: () ->
    storageService.get("session")?.bearerToken or null
  getUser: () ->
    storageService.get("session")?.user or null

  login: (username, password) ->
    credentials = btoa("#{username}:#{password}")
    authHeader = { Authorization: "Basic #{credentials}"}
    $http.get("/api/auth/token/create", { headers: authHeader, preventErrorLogging: true })
      .success((res) =>
        @setSession(res.access_token)
        flashService.success("Welcome, #{@getUser().username}!")
      )
  logout: () ->
    @unsetSession()
    flashService.success("Goodbye!")

  initSession: () ->
    tokenStr = @getBearerToken()
    if tokenStr?
      try
        @setSession(tokenStr)
      catch
        flashService.error("Your session token is invalid")
        @unsetSession()
  setSession: (tokenStr) ->
    token = parseToken(tokenStr)
    user =
      id: token.sub
      username: token.name
    storageService.set("session",
      isAuthenticated: true
      bearerToken: tokenStr
      user: user
    )
    $rootScope.user = user
  unsetSession: () ->
    storageService.set("session",
      isAuthenticated: false
      bearerToken: null
      user: null
    )
    $rootScope.user = null
])

angular.module("app").factory("authService.tokenInjector", ["$injector", ($injector) ->
  request: (config) ->
    authService = $injector.get("authService")
    if authService.isAuthenticated() and not config.headers["Authorization"]?
      config.headers["Authorization"] = "Bearer #{authService.getBearerToken()}"
    config.headers["X-WWW-Authenticate-Filter"] = "Bearer"
    config
])

angular.module("app").factory("authService.tokenRefresher", ["$injector", "$q", ($injector, $q) ->
  responseError: (res) ->
    authService = $injector.get("authService")
    flashService = $injector.get("flashService")
    if res.status == 401
      ah = res.headers("www-authenticate")
      renewCounter = res.config.renewCounter or 0
      if ah? and ah.indexOf("Bearer ") == 0 and ah.indexOf("invalid_token") >= 0 and ah.indexOf("token expired") >= 0 and renewCounter < 1
        deferred = $q.defer()
        config = angular.extend(res.config, { renewCounter: renewCounter + 1 })
        $http = $injector.get("$http")
        $http.get("/api/auth/token/renew").then(deferred.resolve, deferred.reject)

        deferred.promise.then (res2) ->
          if res2.status == 200
            newToken = res2.data.access_token
            authService.setSession(res2.data.access_token)
            delete res.config.headers["Authorization"]
            $http(res.config)
          else
            authService.unsetSession()
            flashService.error("Your session has ended")
      else if ah? and ah.indexOf("Bearer ") == 0 and ah.indexOf("invalid_token") >= 0 and ah.indexOf("token is malformed") >= 0
        authService.unsetSession()
        flashService.error("Your session token is malformed")
        res.config.preventErrorLogging = true
        $q.reject(res)
      else if ah? and ah.indexOf("Bearer ") == 0 and ah.indexOf("invalid_token") >= 0
        authService.unsetSession()
        flashService.error("Your session has ended")
        res.config.preventErrorLogging = true
        $q.reject(res)
      else
        $q.reject(res)
    else
      $q.reject(res)
])
