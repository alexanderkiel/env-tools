(defproject env-tools "0.2-SNAPSHOT"
  :description "Library building nested configuration maps from the environment"
  :url "http://git.life.uni-leipzig.local/clojure/env-tools"

  :min-lein-version "2.0.0"
  :pedantic? :abort

  :dependencies
  [[org.clojure/spec.alpha "0.2.168"]]

  :plugins
  [[jonase/eastwood "0.2.6" :exclusions [org.clojure/clojure]]]

  :profiles
  {:dev
   {:dependencies
    [[org.clojure/clojure "1.9.0"]
     [juxt/iota "0.2.3"]]}
   :clj-1.10
   {:dependencies
    [[org.clojure/clojure "1.10.0-alpha5"]]}}

  :deploy-repositories
  [["life-snapshots"
    {:url "https://portal.life.uni-leipzig.de/content/repositories/snapshots"
     :sign-releases false}]
   ["life-releases"
    {:url "https://portal.life.uni-leipzig.de/content/repositories/releases"
     :sign-releases false}]]

  :aliases
  {"lint" ["eastwood" "{}"]})
