(ns taoensso.trove.timbre
  "Trove -> Timbre backend,
  Ref. <https://www.taoensso.com/timbre>."
  (:require
   [taoensso.trove.utils :as utils]
   [taoensso.timbre      :as timbre]))

(defn get-log-fn
  "Returns a simple log-fn that logs with Timbre.
  Filtering and output will be handled by Timbre.
  Currently no options."
  [{:as _opts}]
  (fn log-fn [ns coords level id lazy_]
    (when (timbre/may-log? level ns)
      (let [{:keys [msg data error #_kvs]}  (force lazy_)]
        (timbre/log!
          {:may-log? true
           :level    level
           :msg-type :p
           :loc      {:ns ns, :line (get coords 0)}
           :?err     error
           :vargs
           (into [] (filter some?)
             [(when id (utils/format-id ns id)) msg data])})))))

(comment ((get-log-fn {}) (str *ns*) [1 2] :info ::id {:msg "msg" :data {:k :v}}))
