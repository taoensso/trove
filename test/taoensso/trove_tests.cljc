(ns taoensso.trove-tests
  (:require
   [clojure.test   :as test :refer [deftest testing is]]
   [taoensso.trove :as trove]
   [taoensso.trove.console]
   [taoensso.trove.telemere]
   [taoensso.trove.timbre]
   #?@
   (:clj
    [[taoensso.trove.mulog]
     [taoensso.trove.tools-logging]
     [taoensso.trove.slf4j]
     [com.brunobonacci.mulog]]))

  #?(:cljs
     (:require-macros
      [taoensso.trove-tests :refer [with-backend]])))

(comment
  (remove-ns      'taoensso.trove-tests)
  (test/run-tests 'taoensso.trove-tests))

;;;; Basics

#?(:clj
   (defmacro with-backend [& body]
     `(let [args_# (atom nil)]
        (binding [trove/*log-fn* (fn [& args#] (reset! args_# (vec args#)))] ~@body)
        @args_#)))

(deftest basics
  [(is (= (with-backend (trove/log! {}))         ["taoensso.trove-tests" [32 25] :info nil                      nil]))
   (is (= (with-backend (trove/log! {:id ::id})) ["taoensso.trove-tests" [33 25] :info :taoensso.trove-tests/id nil]))
   (is (= (with-backend (trove/log! {:ns "ns", :coords [12 34], :data {:k1 :v1}, :k2 :v2}))
         ["ns" [12 34] :info nil {:data {:k1 :v1}, :kvs {:k2 :v2}}]))

   (let [lazy_ (get (with-backend (trove/log! {:msg (str "a" "b" "c")})) 4)]
     [(is    (delay? lazy_))
      (is (= (force  lazy_) {:msg "abc"}))])])

;;;; Backends

(comment
  (do
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
