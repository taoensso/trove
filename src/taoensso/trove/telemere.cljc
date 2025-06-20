(ns taoensso.trove.telemere
  "Trove -> Telemere backend,
  Ref. <https://www.taoensso.com/telemere>."
  (:require [taoensso.telemere :as tel]))

(defn get-log-fn
  "Returns a simple log-fn that creates a Telemere signal.
  Filtering and output will be handled by Telemere.
  Currently no options."
  ([] (get-log-fn nil))
  ([{:as _opts}]
   (fn log-fn:telemere [ns coords level id lazy_]
     (when (tel/signal-allowed? {:kind :trove, :ns ns, :level level, :id id})
       (let [{:keys [msg data error kvs]} (force lazy_)]
         (tel/signal!
           {:allow? true
            :ns     ns
            :coords coords
            :kind   :trove
            :id     id
            :level  level
            :data   data
            :msg    msg
            :error  error
            :kvs+   kvs}))))))

(comment ((get-log-fn) (str *ns*) [1 2] :info ::id {:msg "msg" :data {:k :v}}))
