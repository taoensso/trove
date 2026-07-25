(ns taoensso.trove
  "A minimal, modern logging facade for Clojure/Script.
  Supports both traditional and structured logging."
  {:author "Peter Taoussanis (@ptaoussanis)"}
  (:require
   [taoensso.trove.utils   :as utils]
   [taoensso.trove.console :as console])

  #?(:cljs (:require-macros [taoensso.trove])))

;;;; Dynamic context

(def ^:dynamic *ctx*
  "Optional context map (state) attached to all Trove logs.
  Default (root) value is nil.

  Useful for dynamically attaching arbitrary app-level state to logs.

  Re/bind dynamic        value using `with-ctx`, `with-ctx+`, or `binding`.
  Modify  root (default) value using `set-root-ctx!`.

  As with all dynamic Clojure vars, binding conveyance applies when using
  futures, agents, etc."
  nil)

(defn ^:no-doc update-ctx
  "Returns ?map `new-ctx` given ?map `old-ctx` and an update map or fn."
  [old-ctx update-map-or-fn]
  (cond
    (nil? update-map-or-fn)        old-ctx
    (map? update-map-or-fn) (merge old-ctx update-map-or-fn) ; Before ifn
    (ifn? update-map-or-fn) (update-map-or-fn old-ctx)
    :else
    (throw
      (ex-info "Unexpected context update"
        {:context  `update-ctx
         :param           'update-map-or-fn
         :arg      {:value update-map-or-fn, :type (type update-map-or-fn)}
         :expected '#{nil map fn}}))))

(defmacro set-root-ctx!
  "Sets the root value of `*ctx*` (see its docstring for more info)."
  [root-ctx]
  (if (:ns &env)
    `(set!                *ctx*           ~root-ctx)
    `(alter-var-root (var *ctx*) (fn [_#] ~root-ctx))))

(defmacro with-ctx
  "Evaluates given body with given ?map `*ctx*` value. See `*ctx*` for details."
  [ctx & body]
  `(binding [*ctx* ~ctx] ~@body))

(defmacro with-ctx+
  "Evaluates given body with updated `*ctx*` value.

  `update-map-or-fn` may be:
    - A map to merge with    current `*ctx*` value, or
    - A unary fn to apply to current `*ctx*` value, returning ?map

  See `*ctx*` for details."
  [update-map-or-fn & body]
  `(binding [*ctx* (update-ctx *ctx* ~update-map-or-fn)] ~@body))

;;;; Main API

(def ^:dynamic *log-fn*
  "The value of this var determines the Trove backend,
  i.e. what happens on `trove/log!` calls.

  When `nil`, all `trove/log!` calls will noop.
  Otherwise value should be a (fn [ns coords level id payload_]) with:

    `ns` ------- String namespace  of   `log!` callsite, e.g. \"my-app.utils\"
    `coords` --- ?[line column]    of   `log!` callsite, may be lost (nil) for macros wrapping `log!`

    `level` ----  Keyword `:level` from `log!` call ∈ #{:trace :debug :info :warn :error :fatal :report}
    `id` ------- ?Keyword `:id`    from `log!` call, e.g. `:auth/login`, `::order-complete`, etc.

    `payload_` - {:keys [ctx msg data error kvs]}, MAY be wrapped with `delay` so access with `force`:
      `:ctx` --- ?Map    `*ctx*` value at   `log!` call time
      `:msg` --- ?String `:msg`        from `log!` call
      `:data` -- ?Map    `:data`       from `log!` call, e.g. {:user-id 1234}
      `:error` - ?Error  `:error`      from `log!` call, (`java.lang.Throwable`, `js/Error`, or nil)
      `:kvs` --- ?Map of any other kvs from `log!` call, handy for custom `log-fn` opts, etc.

  The configured `log-fn` may filter (conditionally noop), or produce the
  relevant logging side effects (printing, etc.).

  The configured `log-fn` will be called SYNCHRONOUSLY so:
    - It should implement appropriate async/threading/backpressure for expensive work.
    - It has access to `trove/log!` calling thread/context (can be handy).
    - It has access to `trove/*ctx*` for filtering.

  Config:
    Change dynamic value with `binding`.
    Change root    value with `set-log-fn!`.

    Basic fns are provided for some common backends, see `taoensso.trove.x/get-log-fn`
    with x ∈ #{console telemere timbre mulog tools-logging slf4j} (default console)."

  (console/get-log-fn))

(defmacro set-log-fn!
  "Sets the root value of `*log-fn*` (see its docstring for more info)."
  [f]
  (if (:ns &env)
    `(set!                *log-fn*           ~f)
    `(alter-var-root (var *log-fn*) (fn [_#] ~f))))

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
    `:let` ---- Bindings shared by payload args: {:keys [ctx msg data error kvs]}
    `:ns` ----- Custom namespace string to override default
    `:coords` - Custom [line column]    to override default
    `:ctx` ---- Custom context map      to override default (`*ctx*`)
    `:ctx+` --- Update for `call-time *ctx*`: map to merge or unary fn, trumps `:ctx`
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

  (let [{:keys [ns coords level id msg data error log-fn], letf :let ; forms
         :or
         {ns     (str *ns*)
          level  :info
          coords (utils/callsite-coords &form)
          log-fn `*log-fn*}} opts

        lfn          (gensym "lfn__")
        *ctx*-sym    (gensym "*ctx*__") ; let -> *ctx*
        ctx-sym      (gensym  "ctx__")  ; let ->  ctx-val
        kvs          (not-empty (dissoc opts :ns :coords :level :id :error :let :msg :data :log-fn :ctx :ctx+))
        payload-opts (utils/assoc-some nil {:error error, :msg msg, :data data, :kvs kvs})

        ctx+ (get opts :ctx+)
        ctx-val
        (if ctx+ ; As Telemere: `:ctx+` > `:ctx` > `*ctx*`
          `(update-ctx  ~*ctx*-sym ~ctx+)
          (get opts :ctx *ctx*-sym))

        delay-ctx?     (or ctx+ (and (get opts :ctx) (not (utils/const-form? ctx-val))))
        delay-payload? (or delay-ctx? (not (every? utils/const-form? [payload-opts letf])))

        payload-form
        (if-not payload-opts
          `(let [~ctx-sym ~ctx-val] (when (seq ~ctx-sym) {:ctx ~ctx-sym}))
          `(let [~ctx-sym ~ctx-val
                 payload# ~payload-opts]
             (if (seq ~ctx-sym)
               (assoc payload# :ctx ~ctx-sym)
               (do    payload#))))

        ;; Wrap OUTSIDE ctx binding so that `:let` is visible to `:ctx`/`:ctx+` too
        payload-form (if letf `(let ~letf ~payload-form) payload-form)
        payload-form
        (cond
          delay-payload? `(delay ~payload-form)
          :else                   payload-form)]

    `(let [~lfn ~log-fn]
       (when ~lfn
         (let [~*ctx*-sym *ctx*]
           (~lfn ~ns ~coords ~level ~id ~payload-form)))
       nil)))

(comment
  (do           (log! {:level :info, :msg "msg" :foo :bar}))
  (macroexpand '(log! {:level :info, :msg "msg" :foo :bar})))
