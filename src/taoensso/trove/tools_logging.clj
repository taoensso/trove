(ns taoensso.trove.tools-logging
  "Trove -> tools.logging backend,
  Ref. <https://github.com/clojure/tools.logging>."
  (:require
   [clojure.string       :as str]
   [taoensso.trove.utils :as utils]
   [clojure.tools.logging      :as tl]
   [clojure.tools.logging.impl :as impl]))

(defn get-log-fn
  "Returns a simple log-fn that logs with `tools.logging`.
  `tools.logging` will then delegate to its configured implementation.
  Currently no options."
  ([] (get-log-fn nil))
  ([{:as opts}]
   (fn log-fn:tools-logging [ns coords level id payload_]
     (let [logger (impl/get-logger tl/*logger-factory* ns)]
       (when (impl/enabled? logger level)
         (let [{:keys [ctx msg data error #_kvs]} (force payload_)]
           (tl/log* logger level error
             (str/join " "
               (into [] (filter some?)
                 [(when id (utils/format-id ns id)) msg
                  (when (seq ctx)  (str utils/nl "  ctx: " ctx))
                  (when (seq data) (str utils/nl " data: " data))])))))))))

(comment ((get-log-fn) (str *ns*) [1 2] :info ::id {:msg "line1\nline2" :data {:k :v}}))
