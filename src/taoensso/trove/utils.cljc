(ns taoensso.trove.utils
  "Misc utils, subject to change.")

(def ^:no-doc ^:const nl "System line separator"
  #?(:clj (System/getProperty "line.separator") :cljs "\n"))

(let [cons? (fn [x] (instance? #?(:clj clojure.lang.Cons, :cljs cljs.core/Cons) x))]
  (defn ^:no-doc const-form? [form]
    (cond
      (list? form) false
      (cons? form) false
      (coll? form) (every? const-form? form)
      :else        true)))

(comment (const-form? {:a :A :b :B :c [:d :e :f #_'(str "foo") 'g]}))

(defn callsite-coords
  "Returns [line column] from meta on given macro `&form`."
  [macro-form]
  (when-let [{:keys [line column]} (meta macro-form)]
    (when line (if column [line column] [line]))))

(defn assoc-some
  "Assocs each kv to given ?map iff its value is not nil."
  ([m k v  ] (if-not (nil? v) (assoc m k v) m))
  ([m m-kvs] (reduce-kv assoc-some m m-kvs)))

#?(:bb nil
   :clj
   (defn- reify-log-fn
     "Returns a metadata-friendly wrapper for Trove's 5-arg `log-fn`.
     JVM `with-meta` otherwise wraps fns in a slower generic `RestFn`.
     Wrapper is 5-arity only, so throws `AbstractMethodError` (rather
     than `ArityException`) if misused."
     [^clojure.lang.IFn log-fn]
     (reify
       clojure.lang.Fn
       clojure.lang.IFn
       (applyTo [_ args] (.applyTo log-fn args))
       (invoke  [_ ns coords level id payload_]
         (log-fn   ns coords level id payload_)))))

(defn ^:no-doc assoc-log-fn-meta [log-fn k v]
  (let [m (assoc  (meta log-fn) k v)]
    #?(:bb   (with-meta log-fn m)
       :cljs (with-meta log-fn m)
       :clj  (with-meta (reify-log-fn log-fn) m))))

(defn format-id
  "`:foo.bar/baz` -> \"::baz\", etc."
  [ns x]
  (if (keyword? x)
    (if (= (namespace x) ns)
      (str "::" (name x))
      (str            x))
    (str x)))
