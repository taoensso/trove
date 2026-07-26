(ns taoensso.trove.slf4j
  "Trove -> SLF4J backend,
  Ref. <https://www.slf4j.org/>"
  (:require
   [taoensso.trove       :as trove]
   [taoensso.trove.utils :as utils]))

(defn- qname
  "`:foo.bar/baz` -> \"foo.bar/baz\", etc. Used for all key names, which share
  a namespace with the app's own, so shouldn't carry Clojure's leading colon."
  ^String [x]
  (if (keyword? x)
    (if-let [ns (namespace x)]
      (str ns "/" (name x))
      (do         (name x)))
    (str x)))

(defn- pname
  "\"data\", `:foo.bar/baz` -> \"data.foo.bar/baz\", etc. Prefix identifies the
  source map so that e.g. context and data keys of the same name don't collide
  as duplicate key-values. As Telemere's OpenTelemetry handler."
  ^String [prefix x] (str prefix "." (qname x)))

(defn- put-mdc!
  "Merges given non-empty map into SLF4J's MDC. Returns ?map of the previous
  values of only those keys touched, for later `restore-mdc!`.

  Note that distinct ctx keys may normalize to the same MDC key (e.g. `:a` and
  \"a\"), so keep only the FIRST previous value seen for each - later ones would
  be values we ourselves just put."
  [ctx]
  (reduce-kv
    (fn [acc k v]
      (let [k   (qname k)
            acc (if (contains? acc k) acc (assoc acc k (org.slf4j.MDC/get k)))]
        (org.slf4j.MDC/put k (str v))
        acc))
    {} ctx))

(defn- restore-mdc!
  "Undoes a `put-mdc!`, leaving any other MDC changes intact."
  [prev]
  (reduce-kv
    (fn [_ k v]
      (if v
        (org.slf4j.MDC/put    k v)
        (org.slf4j.MDC/remove k)))
    nil prev))

(defmacro ^:private with-mdc
  "Evals body with given non-empty map merged into SLF4J's MDC, then restores
  only those MDC keys it touched. Unlike a wholesale MDC copy/restore, this
  retains any MDC changes made by body itself (the MDC is imperative, so
  callers reasonably expect their own `MDC/put`s to survive)."
  [ctx & body]
  `(let [prev# (put-mdc! ~ctx)]
     (try ~@body
       (finally (restore-mdc! prev#)))))

(defn get-log-fn
  "Alpha, subject to change (feedback welcome!).
  Returns a simple log-fn that logs with SLF4J.
  Filtering and ouput will be handled by SLF4J.

  Context is provided as key-values, and through SLF4J's MDC so that
  it's also visible to MDC-aware layouts (`%X{my-key}`) encoders, etc.

  Key-value names drop Clojure's leading colon, and carry a prefix
  identifying their source map: `:user-id` in `:data` -> \"data.user-id\",
  in context -> \"ctx.user-id\". MDC names are unprefixed (\"user-id\") so
  that layouts stay legible.

  Options:

  `:bridge-ctx?` (default false)
    Allow `trove/with-ctx-bridge` to merge `trove/*ctx*`
    into native MDC?"

  ([] (get-log-fn nil))
  ([{:keys [bridge-ctx?]}]
   (let [log-fn
         (fn log-fn:slf4j [ns coords level id payload_]
           (let [logger (org.slf4j.LoggerFactory/getLogger (str ns))]
             (when-let [^org.slf4j.spi.LoggingEventBuilder builder
                        (case level
                          :trace          (when (.isTraceEnabled logger) (.atTrace logger))
                          :debug          (when (.isDebugEnabled logger) (.atDebug logger))
                          :info           (when (.isInfoEnabled  logger) (.atInfo  logger))
                          :warn           (when (.isWarnEnabled  logger) (.atWarn  logger))
                          (:error :fatal) (when (.isErrorEnabled logger) (.atError logger))
                          :report                                        (.atInfo  logger)
                          nil)]

               (let [{:keys [ctx msg data error #_kvs]} (force payload_)]
                 (when ns     (.addKeyValue builder "trove/ns"     (str ns)))
                 (when id     (.addKeyValue builder "trove/id"     (str id)))
                 (when coords (.addKeyValue builder "trove/coords" (str coords)))
                 (when msg    (.setMessage  builder                (str msg)))
                 (when error  (.setCause    builder ^Throwable     error))

                 #_(when kvs  (reduce-kv (fn [_ k v] (.addKeyValue builder (pname "kvs"  k) (str v))) nil kvs))
                 (when   data (reduce-kv (fn [_ k v] (.addKeyValue builder (pname "data" k) (str v))) nil data))
                 (when   ctx  (reduce-kv (fn [_ k v] (.addKeyValue builder (pname "ctx"  k) (str v))) nil ctx))

                 (if (empty? ctx) ; Also give ctx to MDC-aware layouts, etc.
                   (do           (.log builder))
                   (with-mdc ctx (.log builder)))))))]

     (if bridge-ctx?
       (trove/add-ctx-bridge log-fn (fn [ctx thunk] (with-mdc ctx (thunk))))
       (do                   log-fn)))))

(comment ((get-log-fn) (str *ns*) [1 2] :info ::id {:msg "line1\nline2" :data {:k :v}}))
