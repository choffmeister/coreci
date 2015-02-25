angular.module("app").service("storageService", () ->
  get: (key) -> JSON.parse(localStorage.getItem(key))
  set: (key, value) -> localStorage.setItem(key, JSON.stringify(value))
  has: (key) -> @get(key)?
  clear: () -> localStorage.clear()
)
