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

(defn resolve-tag [t]
  (cond (symbol? t)
        (if-let [t' (array-type-tags t)]
          (recur t')
          (let [v (resolve t)]
            (when (class? v)
              v)))

        (string? t) (Class/forName t)
        (class? t) t
        (fn? t) (recur (array-fn->array-type t))))

(defn infer-type [&env sym]
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
            (when-let [^Field field (.getField c (name sym))]
              (.getType field))))))))

(defmacro infer* [sym]
  (infer-type &env sym))

(defmacro infer [x]
  (if (symbol? x)
    `(infer* ~x)
    `(let [x# ~x]
       (infer* x#))))
