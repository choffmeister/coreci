angular.module("app").service("flashService", ["$timeout", "$rootScope", ($timeout, $rootScope) ->
  $rootScope.flashMessages = []
  $rootScope.dropFlashMessage = (flashMessage) ->
    index = $rootScope.flashMessages.indexOf(flashMessage)
    $rootScope.flashMessages.splice(index, 1) if index >= 0

  defer = (fn) -> $timeout(() ->
    fn()
  , 0)

  repeat = (fn, delay) -> $timeout(() ->
    fn()
    repeat(fn, delay)
  , delay)

  clean = () ->
    defer () ->
      now = new Date()
      old = _.filter($rootScope.flashMessages, (fm) -> now - fm.timeStamp > 10 * 1000)
      _.each(old, (fm) -> $rootScope.dropFlashMessage(fm))

  msg = (title, message, type) ->
    defer () ->
      $rootScope.flashMessages.push
        type: type
        title: title
        message: message
        timeStamp: new Date()

  repeat(clean, 100)

  success: (title, message) -> msg(title, message, "success")
  info: (title, message) -> msg(title, message, "info")
  warning: (title, message) -> msg(title, message, "warning")
  error: (title, message) -> msg(title, message, "error")

  clear: () -> $rootScope.flashMessages.length = 0
])
