(ns taoensso.trove.console
  "Trove -> console backend."
  (:require
   [clojure.string :as str]
   [taoensso.trove.utils :as utils]))

(defn- level->int ^long [x] (case x :trace 10, :debug 20, :info 50, :warn 60, :error 70, :fatal 80, :report 90, -1))

#?(:cljs (defn- timestamp [] (.toISOString (java.util.Date.)))
   :clj
   (let [formatter (.withZone java.time.format.DateTimeFormatter/ISO_INSTANT java.time.ZoneOffset/UTC)]
     (defn- timestamp [] (.format formatter (java.time.Instant/now)))))

(defn get-log-fn
  "Returns a simple log-fn that:
    - Clj:  logs to `*out*` using `println`.
    - Cljs: logs to JavaScript console.

  Options:
    `:min-level` - ∈ #{nil :trace :debug :info :warn :error :fatal :report},
                   log calls with a lower level will noop."

  ([] (get-log-fn nil))
  ([{:keys [min-level]
     :or   {min-level #?(:clj :info, :cljs nil)}}]

   (fn log-fn:console [ns coords level id lazy_]
     (when #?(:clj true :cljs (exists? js/console))
       (when (or (not min-level) (>= (level->int level) (level->int min-level)))
         (let [{:keys [msg data error #_kvs]} (force lazy_)
               combo-msg
               (str/join " "
                 (into [] (filter some?)
                   [(timestamp) level ns coords
                    (when id (utils/format-id ns id)) msg
                    (when-not (empty? data) (str utils/nl "  data: " data))
                    (when            error  (str utils/nl " error: " error))]))]

           #?(:clj (do (print (str combo-msg utils/nl)) (flush)) ; Atomic println
              :cljs
              (case level
                (:trace :debug) (.debug js/console combo-msg)
                (:info :report) (.info  js/console combo-msg)
                (:warn)         (.warn  js/console combo-msg)
                (:error :fatal) (.error js/console combo-msg)))))))))

(comment ((get-log-fn) (str *ns*) [1 2] :info ::id {:msg "line1\nline2" :data {:k :v}}))
