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
  [{:as opts}]
  (fn log-fn [ns coords level id lazy_]
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
           :data      data
           :kvs       kvs})))))

(comment ((get-log-fn {}) (str *ns*) :info ::id [1 2] {:msg "msg" :data {:k :v}}))
