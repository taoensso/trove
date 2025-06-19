(ns taoensso.trove
  "A minimal, modern logging facade for Clojure/Script.
  Supports both traditional and structured logging."
  {:author "Peter Taoussanis (@ptaoussanis)"}
  (:require
   [taoensso.trove.utils   :as utils]
   [taoensso.trove.console :as console]))

;;;; Main API

(def ^:dynamic *log-fn*
  "The value of this var determines the Trove backend,
  i.e. what happens on `trove/log!` calls.

  When `nil`, all `trove/log!` calls will noop.
  Otherwise value should be a fn of 5 args, (fn [ns coords level id lazy_]):

    `ns` ------- String namespace  of   `log!` callsite, e.g. \"my-app.utils\"
    `coords` --- ?[line column]    of   `log!` callsite, may be lost (nil) for macros wrapping `log!`
    `level` ----  Keyword `:level` from `log!` call ∈ #{:trace :debug :info :warn :error :fatal :report}
    `id` ------- ?Keyword `:id`    from `log!` call, e.g. `:auth/login`, `::order-complete`, etc.
    `lazy_` ---- {:keys [msg data error kvs]}, MAY be wrapped with `delay` so access with `force`:
      `:msg` --- ?String `:msg`        from `log!` call
      `:data` -- ?Map    `:data`       from `log!` call, e.g. {:user-id 1234}
      `:error` - ?Error  `:error`      from `log!` call, (`java.lang.Throwable`, `js/Error`, or nil)
      `:kvs` --- ?Map of any other kvs from `log!` call, handy for custom `log-fn` opts, etc.

  Change dynamic value with `binding`.
  Change root    value with `set-log-fn!`.

  The configured `log-fn` may filter (conditionally noop), or produce the
  relevant logging side effects (printing, etc.).

  NB: `log-fn` is called SYNCHRONOUSLY so:
    - Has access to the calling thread (can be handy).
    - Should implement appropriate async/threading/backpressure
      for expensive work.

  Some common backends are included out-the-box:
    console ------- `taoensso.trove.console/get-log-fn` (default)
    Telemere ------ `taoensso.trove.telemere/get-log-fn`
    Timbre -------- `taoensso.trove.timbre/get-log-fn`
    μ/log --------- `taoensso.trove.mulog/get-log-fn`
    tools.logging - `taoensso.trove.tools-logging/get-log-fn`
    SLF4J --------- `taoensso.trove.slf4j/get-log-fn`"

  (console/get-log-fn {}))

#?(:clj
   (defmacro set-log-fn!
     "Sets the root value of `*log-fn*`.
     See `*log-fn*` for more info."
     [f]
     (if (:ns &env)
       `(set!                *log-fn*           ~f)
       `(alter-var-root (var *log-fn*) (fn [_#] ~f)))))

#?(:clj
   (defmacro log!
     "Logs the given info to the currently configured backend (see `*log-fn*`)
     and returns nil.

     Common options:
       `:level` -- ∈ #{:trace :debug :info :warn :error :fatal :report} (default `:info`)
       `:id` ----- Optional keyword used to identify event, e.g. `:auth/login`, `::order-complete`, etc.
       `:msg` ---- Optional message string describing event (use `str`, `format`, etc. as needed)
       `:data` --- Optional arb map of structured data associated with event, e.g. {:user-id 1234}
       `:error` -- Optional platform error (`java.lang.Throwable`, `js/Error`)

     Advanced options:
       `:ns` ----- Custom namespace string to override default
       `:coords` - Custom [line column]    to override default
       <kvs> ----- Any other kvs will also be provided to `log-fn`, handy for
                   custom `log-fn` opts, etc.

     Traditional logs typically include at least {:keys [level msg ...]}.
     Structured  logs typically include at least {:keys [level id data ...]}."

     [{:keys [ns coords level id msg data error] :as opts ; forms
       :or
       {ns     (str *ns*)
        level  :info
        coords (utils/callsite-coords &form)}}]

     (when-not (map? opts)
       (throw
         (ex-info "Trove opts must be a compile-time map"
           {:opts {:value opts, :type (type opts)}})))

     (let [lfn (gensym "lfn__")
           kvs (not-empty (dissoc opts :ns :coords :level :id :error :msg :data))
           lazy-form
           (when-let [opts (utils/assoc-some nil {:error error, :msg msg, :data data, :kvs kvs})]
             (if (every? utils/const? opts)
               (do      opts) ; Don't pay for wrapping
               `(delay ~opts)))]

       `(let   [~lfn *log-fn*]
          (when ~lfn
            (~lfn ~ns ~coords ~level ~id ~lazy-form))
          nil))))

(comment
  (do           (log! {:level :info, :msg "msg" :foo :bar}))
  (macroexpand '(log! {:level :info, :msg "msg" :foo :bar})))
