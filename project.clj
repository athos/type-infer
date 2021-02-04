(defproject type-infer "0.1.2-SNAPSHOT"
  :description "A Clojure utility to inspect static types inferred by the Clojure compiler"
  :url "https://github.com/athos/type-infer"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :repl-options {:init-ns type-infer.core}
  :profiles {:provided {:dependencies [[org.clojure/clojure "1.10.2"]]}})
