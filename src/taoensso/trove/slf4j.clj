(ns taoensso.trove.slf4j
  "Trove -> SLF4J backend,
  Ref. <https://www.slf4j.org/>"
  (:require [taoensso.trove.utils :as utils]))

#_
(defn- qname ^String [x]
  (if (keyword? x)
    (if-let [ns (namespace x)]
      (str ns "/" (name x))
      (do         (name x)))
    (str x)))

(defn get-log-fn
  "Alpha, subject to change (feedback welcome!).
  Returns a simple log-fn that logs with SLF4J.
  Filtering and ouput will be handled by SLF4J.
  Currently no options."
  [{:as _opts}]
  (fn log-fn [ns coords level id lazy_]
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

        (let [{:keys [msg data error #_kvs]} (force lazy_)]
          (when ns     (.addKeyValue builder ":trove/ns"     (str ns)))
          (when id     (.addKeyValue builder ":trove/id"     (str id)))
          (when coords (.addKeyValue builder ":trove/coords" (str coords)))
          (when msg    (.setMessage  builder                 (str msg)))
          (when error  (.setCause    builder ^Throwable      error))

          #_(when kvs  (reduce-kv (fn [_ k v] (.addKeyValue builder (str k) (str v))) nil kvs))
          (when data   (reduce-kv (fn [_ k v] (.addKeyValue builder (str k) (str v))) nil data))
          (do                                 (.log         builder)))))))

(comment ((get-log-fn {}) (str *ns*) [1 2] :info ::id {:msg "msg" :data {:k :v}}))
