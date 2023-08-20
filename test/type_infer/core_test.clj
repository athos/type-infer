(ns type-infer.core-test
  (:require [clojure.test :refer [deftest is are]]
            [type-infer.core :as ty])
  (:import [java.util Optional]))

(def ^String s "foo")
(def ^ints arr (int-array 0))
(def ^"[J" arr' (long-array 0))

(deftest infer-test
  (is (= Boolean (ty/infer true)))
  (is (= Long/TYPE (ty/infer 42)))
  (is (= String (ty/infer "foo")))
  (is (= clojure.lang.Keyword (ty/infer :bar)))
  (is (= String (ty/infer s)))
  (is (= CharSequence (ty/infer ^CharSequence s)))
  (is (= String (ty/infer java.io.File/separator)))
  (is (= CharSequence (ty/infer ^CharSequence java.io.File/separator)))
  (is (= (Class/forName "[I") (ty/infer arr)))
  (is (= (Class/forName "[J") (ty/infer arr')))
  (let [d 12.3]
    (is (= Double/TYPE (ty/infer d))))
  (let [^doubles arr (make-array (Class/forName "[D") 0)]
    (is (= (Class/forName "[D") (ty/infer arr))))
  (let [arr (make-array (Class/forName "[B") 0)]
    (is (= (Class/forName "[B") (ty/infer ^bytes arr))))
  (is (= Class (ty/infer Optional)))
  (is (= Class (ty/infer java.util.UUID)))
  (is (= Long/TYPE (ty/infer (+ (* 3 3) (* 4 4)))))
  (is (= clojure.lang.Keyword (ty/infer (if (even? 2) :even :odd))))
  (is (= Double/TYPE (ty/infer (let [x 3.0 y (+ x 1)] (+ (* x x) (* y y))))))
  (is (= clojure.lang.AFunction (ty/infer (fn [x] x))))
  (is (nil? (ty/infer (identity 42)))))

(ty/def xs (int-array 0))
(ty/def ^ints ys (identity (int-array 0)))
(ty/def ^"[I" zs (int-array 0))

(deftest def-test
  (is (= (Class/forName "[I") (ty/infer xs)))
  (is (= (Class/forName "[I") (ty/infer ys)))
  (is (= (Class/forName "[I") (ty/infer zs)))
  (let [eval* (fn [expr]
                (binding [*ns* (the-ns 'type-infer.core-test)]
                  (eval expr)))]
    (are [expr] (thrown? Exception (eval* expr))
      '(ty/def ^unknown y "foo")
      '(ty/def x (identity "foo"))
      '(ty/def ^ints vs "foo")
      '(ty/def z 42))))
