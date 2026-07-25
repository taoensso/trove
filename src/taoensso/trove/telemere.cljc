(ns taoensso.trove.telemere
  "Trove -> Telemere backend,
  Ref. <https://www.taoensso.com/telemere>."
  (:require
   [taoensso.trove    :as trove]
   [taoensso.telemere :as tel]))

(defn get-log-fn
  "Returns a simple log-fn that creates a Telemere signal.
  Filtering and output will be handled by Telemere.

  Options:

  `:bridge-ctx?` (default false)
    Allow `trove/with-ctx-bridge` to merge `trove/*ctx*` into
    Telemere's native context?"

  ([] (get-log-fn nil))
  ([{:keys [bridge-ctx?]}]
   (let [log-fn
         (fn log-fn:telemere [ns coords level id payload_]
           (when (tel/signal-allowed? {:kind :trove, :ns ns, :level level, :id id})
             (let [{:keys [ctx msg data error kvs]} (force payload_)]
               (tel/signal!
                 {:allow? true
                  :ns     ns
                  :coords coords
                  :kind   :trove
                  :id     id
                  :level  level
                  :ctx+   ctx
                  :data   data
                  :msg    msg
                  :error  error
                  :kvs+   kvs}))))]

     (if bridge-ctx?
       (trove/add-ctx-bridge log-fn
         (fn [ctx thunk] (tel/with-ctx+ ctx (thunk))))
       log-fn))))

(comment ((get-log-fn) (str *ns*) [1 2] :info ::id {:msg "line1\nline2" :data {:k :v}}))
