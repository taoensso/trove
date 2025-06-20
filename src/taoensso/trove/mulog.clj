(ns taoensso.trove.mulog
  "Trove -> μ/log backend,
  Ref. <https://github.com/BrunoBonacci/mulog>."
  (:require
   [taoensso.trove.utils :as utils]
   [com.brunobonacci.mulog.core :as ml]))

(defn get-log-fn
  "Returns a simple log-fn that logs with μ/log.
  Filtering and output will be handled by μ/log.
  Currently no options."
  ([] (get-log-fn nil))
  ([{:as _opts}]
   (fn log-fn:mulog [ns coords level id lazy_]
     ;; Mulog offers no way to filter here?
     (let [{:keys [msg data error kvs]} (force lazy_)]
       (ml/log* ml/*default-logger*
         (or id :trove/default)
         (utils/assoc-some nil
           {:ns        ns
            :level     level
            :coords    coords
            :msg       msg
            :exception error
            :data      (not-empty data)
            :kvs       (not-empty kvs)}))))))

(comment ((get-log-fn) (str *ns*) [1 2] :info ::id {:msg "msg" :data {:k :v}}))
