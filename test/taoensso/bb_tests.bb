(require
 '[taoensso.trove :as trove]
 '[taoensso.trove.timbre :as timbre])

(trove/log! {:id ::my-d, :msg "msg", :data {:k1 :v1}, :k2 :v2}) ; Default (console) backend

(trove/set-log-fn! (timbre/get-log-fn))

(trove/log! {:id ::my-d, :msg "msg", :data {:k1 :v1}, :k2 :v2})
