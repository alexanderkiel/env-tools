# Env Tools

Env Tools is a library which builds nested configuration maps from a flat list of traditional environment variables. Env Tools uses [Specs][1] to derive the required structure.

## Rationale

TODO

## Install

```clojure
[env-tools "0.1"]
```

## Usage

The assumption is that you have several components in your application, all needing a config map at initialization.

```clojure
(require '[clojure.spec.alpha :as s])

(s/def :config.database/host
  string?)

(s/def :config/database
  (s/keys :req-un [:config.database/host]))

(s/fdef database
  :args (s/cat :config :config/database))

(defn database [config]
  ;; init the database with help of config
  )
```

In the above example, we defined the function `database` which will initialize the database using the supplied config. Furthermore we wrote specs for the `database` function. We named the spec `:config/database` because we like to use it in a `keys` spec describing the whole config as nested map structure.

```clojure
(s/def ::config
  (s/keys :req-un [:config/database]))
```

Above you see the `::config` spec describing a data structure like this:

```clojure
{:database
   {:host "localhost"}}
```

Env Tools provides the function `build-config` which takes a spec and builds a config map from the environment. Assuming an environment like this:

```bash
DATABASE_HOST="localhost"
```

calling:

```clojure
(require '[env-tools.alpha :refer [build-config]])

(build-config ::config)
```

will return:

```clojure
{:database
   {:host "localhost"}}
```

As already said, the structure of the resulting data structure is derived from the specs used. In our example, we used two nested `keys` specs: `(s/keys :req-un [:config/database])` which expects the key `:database` with values following the spec `:config/database` while the `:config/database` spec expects the key `:host` to be a string. The function `build-config` takes those specs and searches the environment for the key `DATABASE_HOST` which is composed of the two keys `:database` and `:host`. If it finds a value, it will return it, otherwise it returns nothing. That said, `build-config` returns at most what your supplied spec defines. You never get the whole environment. So `build-config` is like a query on the environment using spec as a query language.

## Supported Specs

Env Tools interprets your specs in order to extract the right values from the environment. So it has to know how to handle various kinds of specs. The following specs are supported:

TODO

[1]: <https://clojure.org/guides/spec>
