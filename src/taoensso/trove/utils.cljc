(ns taoensso.trove.utils
  "Misc utils, subject to change.")

(def ^:no-doc ^:const nl "System line separator"
  #?(:clj (System/getProperty "line.separator") :cljs "\n"))

#?(:clj
   (let [cons? (fn [x] (instance? clojure.lang.Cons x))]
     (defn ^:no-doc const? [x]
       (cond
         (list? x) false
         (cons? x) false
         (map?  x) (every? const? (vals x))
         (coll? x) (every? const?       x)
         :else     true))))

(comment (const? {:a :A :b :B :c [:d :e :f #_'(str "foo") 'g]}))

#?(:clj
   (defn callsite-coords
     "Returns [line column] from meta on given macro `&form`."
     [macro-form]
     (when-let [{:keys [line column]} (meta macro-form)]
       (when line (if column [line column] [line])))))

(defn assoc-some
  "Assocs each kv to given ?map iff its value is not nil."
  ([m k v  ] (if-not (nil? v) (assoc m k v) m))
  ([m m-kvs] (reduce-kv assoc-some m m-kvs)))

(defn format-id
  "`:foo.bar/baz` -> \"::baz\", etc."
  [ns x]
  (if (keyword? x)
    (if (= (namespace x) ns)
      (str "::" (name x))
      (str            x))
    (str x)))
