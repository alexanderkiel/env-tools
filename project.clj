(defproject env-tools "0.1-SNAPSHOT"
  :description "Library for managing environment variables in Clojure (like Environ)"
  :url "http://git.life.uni-leipzig.local/clojure/env-tools"

  :min-lein-version "2.0.0"
  :pedantic? :abort

  :dependencies
  [[org.clojure/clojure "1.9.0"]]

  :plugins
  [[jonase/eastwood "0.2.6" :exclusions [org.clojure/clojure]]]

  :profiles
  {:dev
   {:dependencies
    [[juxt/iota "0.2.3"]]}}

  :deploy-repositories
  [["life-snapshots"
    {:url "https://portal.life.uni-leipzig.de/content/repositories/snapshots"
     :sign-releases false}]
   ["life-releases"
    {:url "https://portal.life.uni-leipzig.de/content/repositories/releases"
     :sign-releases false}]]

  :aliases
  {"lint" ["eastwood" "{}"]})
