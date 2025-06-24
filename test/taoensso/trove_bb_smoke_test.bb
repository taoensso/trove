(require
 '[taoensso.trove :as trove]
 '[taoensso.trove.timbre :as timbre])

(trove/set-log-fn! (timbre/get-log-fn))

(trove/log! {:level :info :msg "Dude"})
