(defproject org.clojars.akiel/env-tools "0.3.0"
  :description "Library building nested configuration maps from the environment"
  :url "https://github.com/alexanderkiel/env-tools"

  :license {:name "Apache License"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}

  :min-lein-version "2.0.0"
  :pedantic? :abort

  :dependencies
  [[org.clojure/spec.alpha "0.2.176"]]

  :plugins
  [[jonase/eastwood "0.2.6" :exclusions [org.clojure/clojure]]]

  :profiles
  {:dev
   {:dependencies
    [[org.clojure/clojure "1.10.1"]
     [juxt/iota "0.2.3"]]}}

  :aliases
  {"lint" ["eastwood" "{}"]})
