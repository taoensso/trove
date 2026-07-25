(ns taoensso.trove.mulog
  "Trove -> μ/log backend,
  Ref. <https://github.com/BrunoBonacci/mulog>."
  (:require
   [taoensso.trove.utils :as utils]
   [com.brunobonacci.mulog :as mulog]
   [com.brunobonacci.mulog.core :as ml]))

(defn get-log-fn
  "Returns a simple log-fn that logs with μ/log.
  Filtering and output will be handled by μ/log.
  Currently no options."
  ([] (get-log-fn nil))
  ([{:as _opts}]
   (fn log-fn:mulog [ns coords level id payload_]
     ;; Mulog offers no way to filter here?
     (let [{:keys [ctx msg data error kvs]} (force payload_)
           log-data
           (utils/assoc-some nil
             {:ns        ns
              :level     level
              :coords    coords
              :msg       msg
              :exception error
              :data      (not-empty data)
              :kvs       (not-empty kvs)})]

       (if (seq ctx)
         (mulog/with-context ctx (ml/log* ml/*default-logger* (or id :trove/default) log-data))
         (do                     (ml/log* ml/*default-logger* (or id :trove/default) log-data)))))))

(comment ((get-log-fn) (str *ns*) [1 2] :info ::id {:msg "line1\nline2" :data {:k :v}}))
