<img src="https://raw.githubusercontent.com/workframers/morley/master/resources/morley.png">

[![Clojars Project](https://img.shields.io/clojars/v/com.workframe/morley.svg)](https://clojars.org/com.workframe/morley)

Helpful re-frame events for manipulating and fetching store data

### API Overview

#### `morley.core/get`

Read a value from db by `k`, `not-found` or `nil` if value not present.

```clojure
(morley.core/get :my-store-key)
```

#### `morley.core/get-in`

Read a value from db by `path`, `not-found` or `nil` if value not present.

```clojure
(morley.core/get-in [:my-store-key :nested-key])
```

#### `morley.core/assoc`

Applies assoc to app-db with `args`.

```clojure
(morley.core/assoc :my-store-key {:my "cool new value"})
```

#### `morley.core/assoc-in`

Applies assoc-in to app-db with `args`.

```clojure
(morley.core/assoc-in [:my-store-key :nested-key] {:my "cool new value"})
```

#### `morley.core/update`

Applies update to app-db with `args`.

```clojure
(morley.core/update :my-store-key inc)
```

#### `morley.core/update-in`

Applies update-in to app-db with `args`.

```clojure
(morley.core/update-in [:my-store-key :nested-key] inc)
```

#### `morley.core/dissoc`

Applies dissoc to app-db with `args`.

```clojure
(morley.core/dissoc :my-store-key)
```

#### `morley.core/dissoc-in`

Applies dissoc-in to app-db with `args`.

```clojure
(morley.core/dissoc-in [:my-store-key :nested-key] :some-key)
```

#### `morley.core/reset`

Clears the store. Useful for testing.

```clojure
(morley.core/reset)
```

#### `morley.core/watch`

Allows you to pass a `get` or `get-in` value instead of a full subscription, so that it feels more like all the other utils.

```clojure
(morley.core/watch [:my-store-key :nested-key] (fn [_] (do-something)))
```

#### `morley.core/watch-sub`

Given a subscription, call on-change-fn when it changes, passing the old-value and the new-value. Returns an unwatch function for cleanup. This is basically a convenience function around add-watch/remove-watch on re-frame subscriptions.

```clojure
(morley.core/watch-sub (rf/subscribe [:my-subscription])
                       (fn [old new] (do-something)))
```

#### `morley.core/local-storage`

A re-frame interceptor that, after an event, will persist to local storage anything in the app-db that exists under the `:local-storage` key. If the `:sync-before` flag is passed, the interceptor will read from local storage and reset the value at the `:local-storage` key to what was read before running the event handler. This is applied to any Morley 'change' function

```clojure
(morley.core/assoc-in [:local-storage :nested-key] {:my "value"})
```

## License

Copyright © 2018 Workframe, Inc.

Distributed under the Apache License, Version 2.0.
