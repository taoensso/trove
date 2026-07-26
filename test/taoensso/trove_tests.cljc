(ns taoensso.trove-tests
  (:require
   [clojure.string :as str]
   [clojure.test   :as test :refer [deftest testing is]]
   [taoensso.timbre :as timbre]
   [taoensso.trove :as trove]
   [taoensso.trove.console :as trove-console]
   [taoensso.trove.timbre :as trove-timbre]
   #?@
   (:bb []
    :clj
    [[com.brunobonacci.mulog :as mulog]
     [taoensso.telemere :as tel]
     [taoensso.trove.mulog :as trove-mulog]
     [taoensso.trove.slf4j :as trove-slf4j]
     [taoensso.trove.telemere :as trove-telemere]
     [taoensso.trove.tools-logging]]

    :cljs
    [[taoensso.telemere :as tel]
     [taoensso.trove.telemere :as trove-telemere]]))

  #?(:cljs
     (:require-macros
      [taoensso.trove-tests :refer [with-backend]])))

(comment
  (remove-ns      'taoensso.trove-tests)
  (test/run-tests 'taoensso.trove-tests))

;;;; Basics

(defn capturing-log-fn [] (let [args_ (atom nil)] [args_ (fn [& args] (reset! args_ (vec args)))]))
#?(:clj
   (defmacro with-backend [& body]
     `(let [[args_# lfn#] (capturing-log-fn)]
        (binding [trove/*log-fn* lfn#]
          ~@body @args_#))))

(deftest basics
  [(is (= (with-backend (trove/log! {}))         ["taoensso.trove-tests" [41 25] :info nil                      nil]))
   (is (= (with-backend (trove/log! {:id ::id})) ["taoensso.trove-tests" [42 25] :info :taoensso.trove-tests/id nil]))
   (is (= (with-backend (trove/log! {:ns "ns", :coords [12 34], :data {:k1 :v1}, :k2 :v2}))
         ["ns" [12 34] :info nil {:data {:k1 :v1}, :kvs {:k2 :v2}}]))

   (testing "Auto delay wrapping"
     [(let [payload_ (get (with-backend (trove/log! {:msg "abc"})) 4)]
        [(is (not (delay? payload_)))
         (is (=   (force  payload_) {:msg "abc"}))])

      (let [payload_ (get (with-backend (trove/log! {:msg (str "a" "b" "c")})) 4)]
        [(is      (delay? payload_))
         (is (=   (force  payload_) {:msg "abc"}))])])

   (testing ":let option"
     (let [payload_
           (get
             (with-backend
               (trove/log!
                 {:let  [user-id 1234],
                  :data {:user-id     user-id}
                  :msg  (str "User: " user-id)
                  :kv1              #{user-id}}))
             4)]

       [(is    (delay? payload_))
        (is (= (force  payload_) {:msg "User: 1234", :data {:user-id 1234}, :kvs {:kv1 #{1234}}}))]))

   (testing ":log-fn option"
     (is
       (=
         (get (let [[args_ lfn] (capturing-log-fn)] (trove/log! {:msg "Hello!" :log-fn lfn}) @args_) 4)
         {:msg "Hello!"})))])

(deftest context
  [(is (= (trove/with-ctx {:ctx true} :body) :body))

   (let [root-ctx trove/*ctx*]
     (try
       (trove/set-root-ctx! {:root true})
       (is (= trove/*ctx*   {:root true}))
       (finally (trove/set-root-ctx! root-ctx))))

   (let [payload_
         (get
           (trove/with-ctx {:a 1}
             (with-backend (trove/log! {:msg (str "m" "sg")})))
           4)]

     ;; Note `force` here happens *outside* the `with-ctx` scope
     [(is (delay? payload_))
      (is (= (force payload_) {:ctx {:a 1}, :msg "msg"})
        "Ctx snapshot survives realization outside its scope")])

   (is
     (=
       (->
         (trove/with-ctx    {:a 1, :b 1}
           (trove/with-ctx+ {:b 2, :c 2}
             (with-backend (trove/log! {}))))
         (get 4) force)
       {:ctx {:a 1, :b 2, :c 2}}))

   (is
     (=
       (->
         (trove/with-ctx   {:scope :outer}
           (trove/with-ctx {:scope :inner}
             (with-backend (trove/log! {}))))
         (get 4) force)
       {:ctx {:scope :inner}}))

   (is
     (=
       (->
         (trove/with-ctx {:n 1}
           (trove/with-ctx+ #(update % :n inc)
             (with-backend (trove/log! {}))))
         (get 4) force)
       {:ctx {:n 2}}))

   (testing "`log!` call opts"
     (let [ctx-of (fn [args] (:ctx (force (get args 4))))]
       [(is (=     (ctx-of (trove/with-ctx {:a 1} (with-backend (trove/log! {:ctx {:b 2}})))) {:b 2})              "`:ctx` replaces")
        (is (=     (ctx-of (trove/with-ctx {:a 1} (with-backend (trove/log! {:ctx+ {:b 2}})))) {:a 1, :b 2})       "`:ctx+` merges map")
        (is (=     (ctx-of (trove/with-ctx {:n 1} (with-backend (trove/log! {:ctx+ #(update % :n inc)})))) {:n 2}) "`:ctx+` applies fn")
        (is (=     (ctx-of (trove/with-ctx {:a 1} (with-backend (trove/log! {:ctx nil})))) nil) "Explicit nil `:ctx` suppresses ambient context")
        (is (= (force (get (trove/with-ctx {}     (with-backend (trove/log! {}))) 4)) nil)      "Empty context omitted")
        (is (= (force (get (trove/with-ctx {}     (with-backend (trove/log! {:msg "msg"}))) 4)) {:msg "msg"})             "Empty context omitted from payload")
        (is (=     (ctx-of (trove/with-ctx {:a 1} (with-backend (trove/log! {:ctx {:b 2}, :ctx+ {:c 3}})))) {:a 1, :c 3}) "As Telemere, `:ctx+` trumps `:ctx`")
        (is (= (force (get (trove/with-ctx {:a 1} (with-backend (trove/log! {:ctx {:b 2}, :msg "msg"}))) 4)) {:ctx {:b 2}, :msg "msg"}) "Not treated as kvs")]))

   (testing "`:let` bindings are shared by context and payload args"
     [(is
        (=
          (force (get (with-backend (trove/log! {:let [uid 1234], :ctx {:user-id uid}, :msg (str "User: " uid)})) 4))
          {:ctx {:user-id 1234}, :msg "User: 1234"}))

      (is
        (=
          (force
            (get
              (trove/with-ctx {:a 1}
                (with-backend (trove/log! {:let [uid 1234], :ctx+ {:user-id uid}})))
              4))
          {:ctx {:a 1, :user-id 1234}}))

      (let [calls_ (atom 0)
            payload_
            (get (with-backend (trove/log! {:let [uid (do (swap! calls_ inc) 1234)], :ctx {:user-id uid}})) 4)]

        [(is (delay? payload_)      "Delayed when only `:ctx` uses a runtime `:let` binding")
         (is (= @calls_ 0)          "Runtime `:let` binding not evaluated before realization")
         (is (= (force payload_) {:ctx {:user-id 1234}}))
         (is (= @calls_ 1))])])

   (testing "Runtime call context is payload-lazy"
     (let [calls_       (atom [])
           ctx-payload_ (get (with-backend (trove/log! {:ctx (do (swap! calls_ conj :ctx) {:a 1})})) 4)
           ctx+-payload_
           (get
             (trove/with-ctx {:n 1}
               (with-backend
                 (trove/log! {:ctx+ (fn [ctx] (swap! calls_ conj :ctx+) (update ctx :n inc))})))
             4)]

       [(is (= @calls_ []))
        (is (= (force ctx-payload_)  {:ctx {:a 1}}))
        (is (= (force ctx+-payload_) {:ctx {:n 2}}) "Deferred update uses call-time ambient context")
        (is (= @calls_ [:ctx :ctx+]))]))])

(deftest context-bridge
  [(testing "No backend support"
     (is
       (= :body
         (binding [trove/*log-fn* (fn [& _])]
           (trove/with-ctx {:a 1}
             (trove/with-ctx-bridge :body))))))

   (testing "Generic backend support"
     (let [events_ (atom [])
           base-log-fn
           (fn [ns coords level id payload_]
             [ns coords level id payload_])

           log-fn
           (trove/add-ctx-bridge base-log-fn
             (fn [ctx thunk]
                (swap! events_ conj [:enter ctx])
                (try
                  (thunk)
                  (finally (swap! events_ conj [:exit ctx])))))]

       [(is (fn? log-fn))
        (is (= (log-fn :ns :coords :level :id :payload)
              [:ns :coords :level :id :payload]))
        (is (= (apply log-fn [:ns :coords :level :id :payload])
              [:ns :coords :level :id :payload]))
        (is (= :body
              (binding [trove/*log-fn* log-fn]
                (trove/with-ctx-bridge :body))))

        (is (= :body
              (binding [trove/*log-fn* log-fn]
                (trove/with-ctx {}
                  (trove/with-ctx-bridge :body)))))

        (is (empty? @events_))]

       (is
         (= :body
           (binding [trove/*log-fn* log-fn]
             (trove/with-ctx {:a 1}
               (trove/with-ctx-bridge
                 (swap! events_ conj [:body trove/*ctx*])
                 :body)))))

       (is
         (= @events_
           [[:enter {:a 1}]
            [:body  {:a 1}]
            [:exit  {:a 1}]]))))

   #?(:bb nil
      :default
      (testing "Timbre context bridge"
        (is
          (=
            (timbre/with-context+ {:native true, :shared :native}
              (binding [trove/*log-fn* (trove-timbre/get-log-fn {:bridge-ctx? true})]
                (trove/with-ctx {:trove true, :shared :trove}
                  (trove/with-ctx-bridge timbre/*context*))))
            {:native true, :trove true, :shared :trove}))))

   #?(:bb nil
      :default
      (testing "Telemere context bridge"
        (let [log-fn        (trove-telemere/get-log-fn {:bridge-ctx? true})

              disabled-ctx
              (tel/with-ctx {:native true}
                (binding [trove/*log-fn* (trove-telemere/get-log-fn)]
                  (trove/with-ctx {:trove true}
                    (trove/with-ctx-bridge tel/*ctx*))))

              forwarded-signal
              (tel/with-signal true
                (binding [trove/*log-fn* log-fn]
                  (trove/with-ctx {:trove true}
                    (trove/log! {:id ::trove-call}))))

              unbridged-signal
              (tel/with-signal true
                (tel/with-ctx {:native true, :shared :native}
                  (binding [trove/*log-fn* log-fn]
                    (trove/with-ctx {:trove true, :shared :trove}
                      (tel/log! {:id ::native-telemere-call})))))

              bridged-signal
              (tel/with-signal true
                (tel/with-ctx {:native true, :shared :native}
                  (binding [trove/*log-fn* log-fn]
                    (trove/with-ctx {:trove true, :shared :trove}
                      (trove/with-ctx-bridge
                        (tel/log! {:id ::native-telemere-call}))))))]

          [(is (= disabled-ctx {:native true}) "Backend must opt in")
           (is (= (:ctx forwarded-signal) {:trove true}))
           (is (= (:ctx unbridged-signal) {:native true, :shared :native}))
           (is (= (:ctx   bridged-signal)
                 {:native true, :trove true, :shared :trove}))])))])

#?(:bb nil
   :clj
   (deftest context-backends
     [(testing "Console"
        (is
          (str/includes?
            (with-out-str
              (binding [trove/*log-fn* (trove-console/get-log-fn)]
                (trove/with-ctx {:a 1} (trove/log! {}))))
            "ctx: {:a 1}")))

      (testing "μ/log context bridge"
        (is
          (=
            (mulog/with-context {:native true, :shared :native}
              (binding [trove/*log-fn* (trove-mulog/get-log-fn {:bridge-ctx? true})]
                (trove/with-ctx {:trove true, :shared :trove}
                  (trove/with-ctx-bridge (mulog/local-context)))))
            {:native true, :trove true, :shared :trove})))

      (testing "SLF4J context"
        (let [event_ (atom nil)
              appender ; Note: Logback reads the MDC lazily, so capture on append
              (doto
                (proxy [ch.qos.logback.core.AppenderBase] []
                  (append [ev]
                    (let [^ch.qos.logback.classic.spi.ILoggingEvent ev ev]
                      (reset! event_
                        {:mdc (into {} (.getMDCPropertyMap ev))
                         :kvs (into {}
                                (map (fn [^org.slf4j.event.KeyValuePair kv]
                                       [(.-key kv) (.-value kv)]))
                                (.getKeyValuePairs ev))}))))
                (.start))

              ^ch.qos.logback.classic.Logger logger
              (org.slf4j.LoggerFactory/getLogger "slf4j.test.ns")
              additive? (.isAdditive logger)]

          (doto logger
            (.setAdditive false) ; Don't also print to console
            (.addAppender appender))

          (try
            (binding [trove/*log-fn* (trove-slf4j/get-log-fn)]
              (trove/with-ctx {:a 1}
                (trove/log! {:ns "slf4j.test.ns", :msg "msg", :data {:a 2}})))

            [(is (= (:mdc @event_) {"a" "1"}) "Context given to MDC, unprefixed")
             (is (= (get (:kvs @event_) "ctx.a") "1")
               "Context also kept as key-values, for providers without MDC support")
             (is (= (get (:kvs @event_) "data.a") "2")
               "Data key-values are prefixed, so don't collide with context")
             (is (= (get (:kvs @event_) "trove/ns") "slf4j.test.ns")
               "Trove's own key-values drop Clojure's leading colon")]

            (finally
              (.detachAppender logger appender)
              (.setAdditive   logger additive?)))))

      (testing "SLF4J MDC context bridge"
        (let [mdc (fn [] (into {} (org.slf4j.MDC/getCopyOfContextMap)))]
          (try
            (org.slf4j.MDC/put "native" "true")
            (org.slf4j.MDC/put "shared" "native")
            [(is
               (=
                 (binding [trove/*log-fn* (trove-slf4j/get-log-fn {:bridge-ctx? true})]
                   (trove/with-ctx {:trove true, :shared :trove}
                     (trove/with-ctx-bridge (mdc))))
                 {"native" "true", "trove" "true", "shared" ":trove"}))

             (is (= (mdc) {"native" "true", "shared" "native"}) "Restores previous MDC")

             (is
               (=
                 (do
                   (binding [trove/*log-fn* (trove-slf4j/get-log-fn {:bridge-ctx? true})]
                     (trove/with-ctx {:trove true}
                       (try (trove/with-ctx-bridge (throw (ex-info "Boom" {})))
                            (catch Exception _ nil))))
                   (mdc))
                 {"native" "true", "shared" "native"})
               "Restores previous MDC on exception")

             (is
               (=
                 (do
                   (binding [trove/*log-fn* (trove-slf4j/get-log-fn {:bridge-ctx? true})]
                     (trove/with-ctx {:shared 1, "shared" 2} ; Both -> "shared"
                       (trove/with-ctx-bridge nil)))
                   (mdc))
                 {"native" "true", "shared" "native"})
               "Restores previous MDC given keys that normalize alike")

             ;; Note: deliberately leaves "body" in the MDC, so keep last
             (is
               (=
                 (do
                   (binding [trove/*log-fn* (trove-slf4j/get-log-fn {:bridge-ctx? true})]
                     (trove/with-ctx {:trove true}
                       (trove/with-ctx-bridge (org.slf4j.MDC/put "body" "true"))))
                   (mdc))
                 {"native" "true", "shared" "native", "body" "true"})
               "Retains MDC changes made by body")]

            (finally (org.slf4j.MDC/clear)))))]))

;;;; Backends

(comment
  (do
    (require '[taoensso.trove.telemere])
    #?(:clj
       (require
         '[taoensso.trove.mulog]
         '[taoensso.trove.slf4j]
         '[com.brunobonacci.mulog]))

    (defn- log1! [] (trove/log! {:id ::my-d, :msg "msg", :data {:k1 :v1}, :k2 :v2}))
    (com.brunobonacci.mulog/start-publisher! {:type :console})

    (with-out-str (binding [trove/*log-fn* (taoensso.trove.console/get-log-fn)]       (log1!)))
    (with-out-str (binding [trove/*log-fn* (taoensso.trove.telemere/get-log-fn)]      (log1!)))
    (with-out-str (binding [trove/*log-fn* (taoensso.trove.timbre/get-log-fn)]        (log1!)))
    (do           (binding [trove/*log-fn* (taoensso.trove.mulog/get-log-fn)]         (log1!)))
    (do           (binding [trove/*log-fn* (taoensso.trove.tools-logging/get-log-fn)] (log1!)))
    (do           (binding [trove/*log-fn* (taoensso.trove.slf4j/get-log-fn)]         (log1!)))))

;;;;

#?(:cljs
   (defmethod test/report [:cljs.test/default :end-run-tests] [m]
     (when-not (test/successful? m)
       ;; Trigger non-zero `lein test-cljs` exit code for CI
       (throw (ex-info "ClojureScript tests failed" {})))))

#?(:cljs (test/run-tests))
