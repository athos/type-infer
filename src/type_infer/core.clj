(ns type-infer.core
  (:import [clojure.lang Compiler$LocalBinding]
           [java.lang.reflect Field]))

(def ^:private array-type-tags
  '{booleans "[Z", bytes "[B", chars "[C"
    shorts "[S", ints "[I", longs "[J"
    floats "[F", doubles "[D"
    objects "[Ljava.lang.Object;"})

(def ^:private array-fn->array-type
  {booleans 'booleans, bytes 'bytes, chars 'chars
   shorts 'shorts, ints 'ints, longs 'longs
   floats 'floats, doubles 'doubles})

(defn resolve-tag ^Class [t]
  (cond (symbol? t)
        (if-let [t' (array-type-tags t)]
          (recur t')
          (let [v (resolve t)]
            (when (class? v)
              v)))

        (string? t)
        (try
          (Class/forName t)
          (catch Exception _ nil))

        (class? t) t
        (fn? t) (recur (array-fn->array-type t))))

(defn infer-type ^Class [&env sym]
  (if-let [t (:tag (meta sym))]
    (resolve-tag t)
    (if-let [^Compiler$LocalBinding lb (get &env sym)]
      (when (.hasJavaClass lb)
        (.getJavaClass lb))
      (if-let [v (resolve sym)]
        (cond (var? v)
              (when-not (instance? clojure.lang.AFunction @v)
                (when-let [t (:tag (meta v))]
                  (resolve-tag t)))

              (class? v)
              Class)
        (when-let [c (some-> (namespace sym) symbol resolve)]
          (when (class? c)
            (when-let [^Field field (.getField ^Class c (name sym))]
              (.getType field))))))))

(defmacro infer* [sym]
  (infer-type &env sym))

(defmacro infer [x]
  (if (symbol? x)
    `(infer* ~x)
    `(let [x# ~x]
       (infer* x#))))

(defmacro def
  "Drop-in replacement macro for Clojure's def.
  Unlike Clojure's def, this macro infers the static type of `init` and
  automatically adds the inferred type as the type hint for the var to be defined.
  If the type can't be inferred statically, an error will be thrown at macro
  expansion time.
  Optionally, a type hint can be specified. In that case, the hinted type and
  the inferred type must be compatible (if it's inferred)."
  {:added "0.2.0"
   :clj-kondo/lint-as 'clj-kondo.lint-as/def-catch-all}
  ([name init]
   (with-meta
     `(type-infer.core/def ~name nil ~init)
     (meta &form)))
  ([name docstr init]
   (if (symbol? init)
     (with-meta `(def* ~name ~init) (meta &form))
     (let [sym (gensym 'init)]
       `(let [~sym ~init]
          ~(with-meta
             `(def* ~name ~sym ~docstr ~init)
             (meta &form)))))))

(defmacro def* [name sym docstr expr]
  (let [inferred-type (infer-type &env sym)
        tag (:tag (meta name))
        ^Class hinted-type (some-> tag resolve-tag)]
    (cond (and tag (nil? hinted-type))
          (let [msg (format "Unable to resolve tag: %s in this context"
                            (pr-str tag))]
            (throw (ex-info msg {:hinted-tag tag})))

          (and (nil? inferred-type) (nil? hinted-type))
          (let [msg (format "Can't infer static type of %s" (pr-str expr))]
            (throw (ex-info msg {})))

          (and hinted-type
               inferred-type
               (not (.isAssignableFrom hinted-type inferred-type)))
          (let [msg (format "Inferred type (%s) is not compatible with hinted type (%s)"
                            (.getName inferred-type)
                            (.getName hinted-type))]
            (throw (ex-info msg {:hinted-type hinted-type
                                 :inferred-type inferred-type})))

          (and inferred-type (.isPrimitive inferred-type))
          (let [msg "Primitive types are not supported. Use Clojure's def instead."]
            (throw (ex-info msg {:inferred-type inferred-type})))

          :else
          `(def ~(vary-meta name assoc :tag (or hinted-type inferred-type))
             ~@(when docstr [docstr])
             ~sym))))
