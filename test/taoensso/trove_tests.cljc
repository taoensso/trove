(ns taoensso.trove-tests
  (:require
   #?(:clj  [clojure.core :as core]
      :cljs [cljs.core    :as core])
   [clojure.test   :as test :refer [deftest testing is]]))

(comment
  (remove-ns      'taoensso.trove-tests)
  (test/run-tests 'taoensso.trove-tests))

;;;;

;; TODO Tests

;;;;

#?(:cljs
   (defmethod test/report [:cljs.test/default :end-run-tests] [m]
     (when-not (test/successful? m)
       ;; Trigger non-zero `lein test-cljs` exit code for CI
       (throw (ex-info "ClojureScript tests failed" {})))))

#?(:cljs (test/run-tests))
