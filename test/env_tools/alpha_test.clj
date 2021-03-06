(ns env-tools.alpha-test
  (:require
    [clojure.spec.alpha :as s]
    [clojure.spec.test.alpha :as st]
    [clojure.test :refer :all]
    [env-tools.alpha :refer [build-config]]
    [juxt.iota :refer [given]]))

(st/instrument)

(s/def ::host
  string?)

(s/def ::host-only
  (s/keys :req-un [::host]))

(s/def ::port
  string?)

(s/def ::database
  (s/keys :req-un [::host ::port]))

(s/def ::with-database
  (s/keys :req-un [::database]))

(s/def ::token-expire
  string?)

(s/def ::token-expire-only
  (s/keys :req-un [::token-expire]))

(s/def ::mail
  (s/keys :req-un [::host]))

(s/def ::with-database-and-mail
  (s/keys :req-un [::database ::mail]))

(defmulti kv-store :type)

(s/def ::bucket
  string?)

(defmethod kv-store "riak" [_]
  (s/keys :req-un [::host ::bucket]))

(s/def ::key-space
  string?)

(defmethod kv-store "cassandra" [_]
  (s/keys :req-un [::host ::key-space]))

(s/def ::kv-store
  (s/multi-spec kv-store :type))

(s/def ::with-multi-spec
  (s/keys :req-un [::kv-store]))

(s/def ::token
  string?)

(s/def ::tokens
  (s/coll-of ::token))

(s/def ::with-coll-of-strings
  (s/keys :req-un [::tokens]))

(s/def ::databases
  (s/coll-of ::database))

(s/def ::with-coll-of-databases
  (s/keys :req-un [::databases]))

(s/def :database/host
  string?)

(s/def :database/port
  string?)

(s/def :nsq/database
  (s/keys :req [:database/host :database/port]))

(s/def ::with-nsq-database
  (s/keys :req-un [:nsq/database]))

(s/def ::issue-1
  (s/keys :req-un [(and :database/host :database/port)]))

(deftest build-config-test
  (testing "Returns empty map on non-matching env vars."
    (is (map? (build-config ::host-only {}))))

  (testing "Simple `keys` spec"
    (given (build-config ::host-only {["HOST"] "host-140945"})
      :host := "host-140945"))

  (testing "Nested `keys` specs"
    (given (build-config ::with-database {["DATABASE" "HOST"] "host-140954"})
      [:database :host] := "host-140954"))

  (testing "Spec with dash in key"
    (given (build-config ::token-expire-only {["TOKEN" "EXPIRE"] "3600"})
      :token-expire := "3600"))

  (testing "Env vars are shared in nested structures"
    (testing "HOST var is used in database and mail"
      (given (build-config ::with-database-and-mail {["HOST"] "host-141407"})
        [:database :host] := "host-141407"
        [:mail :host] := "host-141407"))

    (testing "Nested DATABASE HOST has precedence over top-level HOST, were :mail :host is fitted with the top-level HOST."
      (given
        (build-config
          ::with-database-and-mail
          {["HOST"] "host-141110"
           ["DATABASE" "HOST"] "database-host-141116"})
        [:database :host] := "database-host-141116"
        [:mail :host] := "host-141110")))

  (testing "multi specs"
    (testing "riak kv-store"
      (given
        (build-config
          ::with-multi-spec
          {["KV" "STORE" "TYPE"] "riak"
           ["KV" "STORE" "HOST"] "host-143129"
           ["KV" "STORE" "BUCKET"] "bucket-143151"})
        [:kv-store :type] := "riak"
        [:kv-store :host] := "host-143129"
        [:kv-store :bucket] := "bucket-143151"))

    (testing "cassandra kv-store"
      (given
        (build-config
          ::with-multi-spec
          {["KV" "STORE" "TYPE"] "cassandra"
           ["KV" "STORE" "HOST"] "host-143613"
           ["KV" "STORE" "KEY" "SPACE"] "key-space-143648"})
        [:kv-store :type] := "cassandra"
        [:kv-store :host] := "host-143613"
        [:kv-store :key-space] := "key-space-143648")))

  (testing "collection of strings"
    (given
      (build-config
        ::with-coll-of-strings
        {["TOKENS" "1"] "token-1-150701"
         ["TOKENS" "2"] "token-2-151952"
         ["TOKENS" "3"] "token-3-151954"})
      :tokens := ["token-1-150701" "token-2-151952" "token-3-151954"]))

  (testing "collection of nested maps"
    (given
      (build-config
        ::with-coll-of-databases
        {["DATABASES" "1" "HOST"] "host-1-155343"
         ["DATABASES" "1" "PORT"] "port-1-155508"
         ["DATABASES" "2" "HOST"] "host-2-155354"
         ["DATABASES" "2" "PORT"] "port-2-155503"})
      [:databases first :host] := "host-1-155343"
      [:databases first :port] := "port-1-155508"
      [:databases second :host] := "host-2-155354"
      [:databases second :port] := "port-2-155503"))

  (testing "namespace qualified key is found top-level"
    (given (build-config ::with-nsq-database {["DATABASE" "HOST"] "host-114151"})
      [:database :database/host] := "host-114151"))

  (testing "namespace qualified key is found under DATABASE"
    (given (build-config ::with-nsq-database {["DATABASE" "DATABASE" "HOST"] "host-114158"})
      [:database :database/host] := "host-114158"))

  (testing "Issue 1"
    (given (build-config ::issue-1 {}))))
