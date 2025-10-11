(ns taoensso.trove
  "A minimal, modern logging facade for Clojure/Script.
  Supports both traditional and structured logging."
  {:author "Peter Taoussanis (@ptaoussanis)"}
  (:require
   [taoensso.trove.utils   :as utils]
   [taoensso.trove.console :as console])

  #?(:cljs (:require-macros [taoensso.trove])))

;;;; Main API

(def ^:dynamic *log-fn*
  "The value of this var determines the Trove backend,
  i.e. what happens on `trove/log!` calls.

  When `nil`, all `trove/log!` calls will noop.
  Otherwise value should be a (fn [ns coords level id lazy_]) with:

    `ns` ------- String namespace  of   `log!` callsite, e.g. \"my-app.utils\"
    `coords` --- ?[line column]    of   `log!` callsite, may be lost (nil) for macros wrapping `log!`

    `level` ----  Keyword `:level` from `log!` call ∈ #{:trace :debug :info :warn :error :fatal :report}
    `id` ------- ?Keyword `:id`    from `log!` call, e.g. `:auth/login`, `::order-complete`, etc.

    `lazy_` ---- {:keys [msg data error kvs]}, MAY be wrapped with `delay` so access with `force`:
      `:msg` --- ?String `:msg`        from `log!` call
      `:data` -- ?Map    `:data`       from `log!` call, e.g. {:user-id 1234}
      `:error` - ?Error  `:error`      from `log!` call, (`java.lang.Throwable`, `js/Error`, or nil)
      `:kvs` --- ?Map of any other kvs from `log!` call, handy for custom `log-fn` opts, etc.

  The configured `log-fn` may filter (conditionally noop), or produce the
  relevant logging side effects (printing, etc.).

  The configured `log-fn` will be called SYNCHRONOUSLY so:
    - It has access to the `trove/log!` calling thread/context (can be handy).
    - It should implement appropriate async/threading/backpressure for
      expensive work.

  Config:

    Change dynamic value with `binding`.
    Change root    value with `set-log-fn!`.

    Basic fns are provided for some common backends, see `taoensso.trove.x/get-log-fn`
    with x ∈ #{console telemere timbre mulog tools-logging slf4j} (default console)."

  (console/get-log-fn))

#?(:clj
   (defmacro set-log-fn!
     "Sets the root value of `*log-fn*` (see its docstring for more info)."
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
       `:let` ---- Bindings shared by lazy args: {:keys [msg data error kvs]}
       `:ns` ----- Custom namespace string to override default
       `:coords` - Custom [line column]    to override default
       `:log-fn` - Custom `log-fn`         to override default (`*log-fn*`)
       <kvs> ----- Any other kvs will also be provided to `log-fn`, handy for
                   custom `log-fn` opts, etc.

     Traditional logs typically include at least {:keys [level msg ...]}.
     Structured  logs typically include at least {:keys [level id data ...]}."

     {:arglists '([{:keys [level id msg data error]}])} ; Common only
     [opts]

     (when-not (map? opts)
       (throw
         (ex-info "Trove opts must be a compile-time map"
           {:opts {:value opts, :type (type opts)}})))

     (let [{:keys [ns coords level id msg data error log-fn] letf :let ; forms
            :or
            {ns     (str *ns*)
             level  :info
             coords (utils/callsite-coords &form)
             log-fn `*log-fn*}} opts

           lfn (gensym "lfn__")
           kvs (not-empty (dissoc opts :ns :coords :level :id :error :let :msg :data :log-fn))
           lazy-form
           (when-let [opts (utils/assoc-some nil {:error error, :msg msg, :data data, :kvs kvs})]
             (if (every? utils/const? [opts letf])
               (if letf        `(let ~letf ~opts)           opts) ; Don't pay for wrapping
               (if letf `(delay (let ~letf ~opts)) `(delay ~opts))))]

       `(let   [~lfn ~log-fn]
          (when ~lfn
            (~lfn ~ns ~coords ~level ~id ~lazy-form))
          nil))))

(comment
  (do           (log! {:level :info, :msg "msg" :foo :bar}))
  (macroexpand '(log! {:level :info, :msg "msg" :foo :bar})))
