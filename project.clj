(defproject org.clojars.akiel/env-tools "0.2.1"
  :description "Library building nested configuration maps from the environment"
  :url "http://git.life.uni-leipzig.local/clojure/env-tools"

  :min-lein-version "2.0.0"
  :pedantic? :abort

  :dependencies
  [[org.clojure/spec.alpha "0.2.176"]]

  :plugins
  [[jonase/eastwood "0.2.6" :exclusions [org.clojure/clojure]]]

  :profiles
  {:dev
   {:dependencies
    [[org.clojure/clojure "1.10.0"]
     [juxt/iota "0.2.3"]]}}

  :aliases
  {"lint" ["eastwood" "{}"]})
