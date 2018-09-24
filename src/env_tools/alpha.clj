(ns env-tools.alpha
  (:require
    [clojure.spec.alpha :as s]
    [clojure.string :as str]))

(s/def ::env-key-seq
  (s/coll-of (s/and string? #(re-matches #"[A-Z0-9]*" %))))

(s/def ::prepared-env
  (s/map-of ::env-key-seq string?))

(def prepare-env
  "Transducer which splits string keys in kv-pairs by underscore."
  (map
    (fn [[k v]]
      [(str/split (str/upper-case k) #"_") v])))

(defn- envize
  "Converts strings to uppercase and splits them by dash or period.

  The resulting sequence fits the key sequences from `prepare-env`."
  [s]
  (-> s str/upper-case (str/split #"[-.]")))

(s/fdef sub-env
  :args (s/cat :prefix ::env-key-seq :env (s/nilable ::prepared-env))
  :ret (s/nilable ::prepared-env))

(defn- sub-env
  "Builds a sub env containing all keys from `env` with matching `prefix`.

  Truncates the prefix from the keys. The `env` should be build with
  `prepare-env`."
  [prefix env]
  (reduce-kv
    (fn [sub-env k v]
      (cond
        (= prefix k)
        (reduced v)
        (= prefix (take (count prefix) k))
        (assoc sub-env (drop (count prefix) k) v)
        :else
        sub-env))
    nil
    env))

(def ^:private filter-numeric-key-start
  "Filters kv-pairs for env-keys starting with numbers."
  (filter (fn [[[x]]] (and x (re-matches #"\d+" x)))))

(defn- remove-kw-ns [k]
  (-> k name keyword))

(defn- multi-spec
  "Returns the spec from a multi-spec."
  [multi-method-sym tag-key tag]
  (try
    (eval (list multi-method-sym {tag-key tag}))
    (catch Exception _)))

(s/fdef build-config
  :args (s/cat :spec some? :env (s/? ::prepared-env)))

(defn build-config
  "Builds a config from `env` according to `spec`.

  A config is a possibly nested map with keyword keys and typed values, were
  `env` is a flat map with string keys and string values.

  If `env` is omitted, `(System/getenv)` is used. Otherwise the `env` has to
  be prepared with `prepare-env` before calling `build-config`. It's important
  that the prepared env keys are sequences of uppercase alphanumeric strings.

  The resulting config reassembles the structure of the `spec` but doesn't
  necessarily conform to the `spec`.

  The following spec forms are supported:

   * `keys` - the resulting config will be a map
   * `multi-spec` - the spec resulting from dispatching the multi-spec is used
   * `coll-of` - all env keys starting with numerical parts are used. Numbers
                 don't have to start with `0` nor have they be without gab. The
                 collection is build by sorting available numbers.

  Supported spec forms can be nested arbitrarily."
  ([spec]
   (build-config spec (into {} prepare-env (System/getenv))))
  ([spec env]
   (let [form (s/form spec)]
     (when (sequential? form)
       (let [[sym & rest-form] form]
         (condp = sym

           ;; build a sub-env for every key in keys
           ;; sub-env will be either maps or scalar
           `s/keys
           (reduce
             (fn [config [operator keys]]
               (case operator
                 (:req :opt)
                 (reduce
                   (fn [config key]
                     (let [prefix (into (envize (namespace key)) (envize (name key)))
                           sub-env (sub-env prefix env)]
                       (if (or (map? sub-env) (nil? sub-env))
                         (if-let [sub-config (build-config key (merge env sub-env))]
                           (assoc config key sub-config)
                           config)
                         (assoc config key sub-env))))
                   config
                   keys)
                 (:req-un :opt-un)
                 (reduce
                   (fn [config key]
                     (let [sub-env (sub-env (envize (name key)) env)]
                       (if (or (map? sub-env) (nil? sub-env))
                         (if-let [sub-config (build-config key (merge env sub-env))]
                           (assoc config (remove-kw-ns key) sub-config)
                           config)
                         (assoc config (remove-kw-ns key) sub-env))))
                   config
                   keys)))
             {}
             (partition 2 rest-form))

           ;; look into the multi-spec by extracting the tag from env
           ;; recur with found spec if any
           `s/multi-spec
           (let [[mm-sym tag-key] rest-form
                 tag (get env (envize (name tag-key)))]
             (some-> (multi-spec mm-sym tag-key tag)
                     (build-config env)
                     (assoc tag-key tag)))

           ;; looks for keys starting with numeric parts and groups them
           ;; for each numeric group, builds a sub-env omitting the numeric part
           ;; for each sub-env, recurs with the coll-spec
           `s/coll-of
           (->> (into [] filter-numeric-key-start env)
                (sort-by ffirst)
                (partition-by ffirst)
                (map
                  (fn [kv-pairs]
                    (reduce
                      (fn [env [[_ & kps] v]]
                        (if (seq? kps)
                          (assoc env kps v)
                          (reduced v)))
                      nil
                      kv-pairs)))
                (map
                  (fn [sub-env]
                    (if (or (map? sub-env) (nil? sub-env))
                      (build-config (first rest-form) (merge env sub-env))
                      sub-env))))
           nil))))))

(defn list-env-vars
  ([spec]
   (list-env-vars spec [] true))
  ([spec prefixes required?]
   (let [form (s/form spec)]
     (if (sequential? form)
       (let [[sym & rest-form] form]
         (condp = sym
           `s/keys
           (into
             []
             (mapcat
               (fn [[operator keys]]
                 (case operator
                   (:req :opt)
                   []
                   (:req-un :opt-un)
                   (into
                     []
                     (mapcat
                       (fn [key]
                         (let [prefixes (into prefixes (envize (name key)))]
                           (list-env-vars key prefixes (= :req-un operator)))))
                     keys))))
             (partition 2 rest-form))

           `s/multi-spec
           (into
             []
             (mapcat
               (fn [method]
                 (list-env-vars (method {}) prefixes required?)))
             (vals (methods (eval (first rest-form)))))

           `s/coll-of
           (list-env-vars (first rest-form) (conj prefixes "1") required?)

           [{:var (str/join "_" prefixes)
             :spec form
             :required? required?}]))
       [{:var (str/join "_" prefixes)
         :spec form
         :required? required?}]))))
