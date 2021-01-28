# type-infer
[![Clojars Project](https://img.shields.io/clojars/v/type-infer.svg)](https://clojars.org/type-infer)

A Clojure utility to inspect static types inferred by the Clojure compiler

## Installation

Add the following to your project `:dependencies`:

[![Clojars Project](https://clojars.org/type-infer/latest-version.svg)](https://clojars.org/type-infer)

## Usage

### `infer`

The `infer` macro tells us the static type of the given expression inferred
by the Clojure compiler:

```clojure
(require '[type-infer.core :refer [infer]])

(infer 42) ;=> long
(infer false) ;=> java.lang.Boolean
(infer "foo") ;=> java.lang.String
```

It can take one arbitrary expression and returns its static type as long as 
the Clojure compiler can infer it:

```clojure
(infer (not false)) ;=> java.lang.Boolean
(infer (/ 1 2)) ;=> java.lang.Number
(infer (if (even? 2) :even :odd)) ;=> clojure.lang.Keyword
(infer (let [x 2 y (inc x)] (* x y))) ;=> long
(infer (fn [x] x)) ;=> clojure.lang.AFunction
```

Otherwise (i.e. the compiler failed to infer the static type of the given expression),
`infer` returns `nil`:

```clojure
(infer (identity 42)) ;=> nil
(infer (if (even? 2) "foo" false)) ;=> nil
(infer cons) ;=> nil
```

The `infer` macro aims to be used mainly as a development tool for performance tuning,
especially when eliminating reflection warnings or taking full advantage of primitive types.

In most cases, `infer` per se is hard to use in your function or macro.
If you want to use it in your macro, it's highly recommend to use `infer-type` below instead.

### `infer-type`

The `infer-type` fn takes two arguments. The first one is the macro's implicit argument
`&env` and the second one is a symbol. To get `&env`, you need to call it in a macro:

```clojure
(require '[type-infer.core :as ty])

(defmacro my-infer-type* [sym]
  (ty/infer-type &env sym))
 
(def ^String s "foo")
(my-infer-type s) ;=> java.lang.String

(let [x 42]
  (my-infer-type x)) ;=> long
```

Note that `infer-type` only accepts a symbol as its second argument.
If you want to pass arbitrary expression to it, you'll need to pass it via a `let` form
like the following:

```clojure
(defmacro my-infer-type [x]
  `(let [x# ~x]
     (my-infer-type* x#)))

(my-infer-type (fn [x] x)) ;=> clojure.lang.AFunction
```

Note also that due to the above limitation, the target expression of the type inference
may be evaluated at most once. This means that it is impossible (or difficult at best)
to implement macros using `infer-type` such that the original expression is never evaluated
in the expanded form, e.g. constant folding.

## License

Copyright © 2021 Shogo Ohta

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
