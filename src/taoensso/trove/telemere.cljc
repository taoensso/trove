(ns taoensso.trove.telemere
  "Trove -> Telemere backend,
  Ref. <https://www.taoensso.com/telemere>."
  (:require [taoensso.telemere :as tel]))

(defn get-log-fn
  "Returns a simple log-fn that creates a Telemere signal.
  Filtering and output will be handled by Telemere.
  Currently no options."
  [{:as _opts}]
  (fn log-fn [ns coords level id lazy_]
    (when (tel/signal-allowed? {:kind :trove, :ns ns, :level level, :id id})
      (let [{:keys [msg data error kvs]} (force lazy_)]
        (tel/signal!
          {:allow? true
           :kind   :trove
           :ns     ns
           :level  level
           :id     id
           :coords coords
           :msg    msg
           :error  error
           :data   data
           :kvs+   kvs})))))

(comment ((get-log-fn {}) (str *ns*) :info ::id [1 2] {:msg "msg" :data {:k :v}}))
