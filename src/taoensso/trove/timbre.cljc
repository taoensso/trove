(ns taoensso.trove.timbre
  "Trove -> Timbre backend,
  Ref. <https://www.taoensso.com/timbre>."
  (:require
   [taoensso.trove       :as trove]
   [taoensso.trove.utils :as utils]
   [taoensso.timbre      :as timbre]))

(defn- log!* [ns coords level id msg data error]
  (timbre/log!
    {:may-log? true
     :level    level
     :msg-type :p
     :loc      {:ns ns, :line (get coords 0)}
     :?err     error
     :vargs
     (into [] (filter some?)
       [(when id (utils/format-id ns id)) msg
        (when (seq data) (str utils/nl " data: " data))])}))

(defn get-log-fn
  "Returns a simple log-fn that logs with Timbre.
  Filtering and output will be handled by Timbre.
  Context is ignored on Babashka, whose bundled Timbre lacks context support.

  Options:

  `:bridge-ctx?` (default false)
    Allow `trove/with-ctx-bridge` to merge `trove/*ctx*` into
    Timbre's native context? Unavailable on Babashka."

  ([] (get-log-fn nil))
  ([{:keys [bridge-ctx?]}]
   (let [log-fn
         (fn log-fn:timbre [ns coords level id payload_]
           (when (timbre/may-log? level ns)
             (let [{:keys [ctx msg data error #_kvs]} (force payload_)]
               #?(:bb (log!* ns coords level id msg data error) ; No context support in BB
                  :default
                  (if (seq ctx)
                    (timbre/with-context+ ctx (log!* ns coords level id msg data error))
                    (do                       (log!* ns coords level id msg data error)))))))]

     #?(:bb log-fn
        :default
        (if bridge-ctx?
          (trove/add-ctx-bridge log-fn
            (fn [ctx thunk] (timbre/with-context+ ctx (thunk))))
          log-fn)))))

(comment ((get-log-fn) (str *ns*) [1 2] :info ::id {:msg "line1\nline2" :data {:k :v}}))
