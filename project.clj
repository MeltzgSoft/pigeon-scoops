(defproject pigeon-scoops "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :min-lein-version "2.9.1"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.12.0"]
                 [thheller/shadow-cljs "2.28.20"]
                 [com.pitch/uix.core "1.3.1"]
                 [com.pitch/uix.dom "1.3.1"]
                 [cljs-ajax "0.8.4"]
                 [binaryage/devtools "1.0.7"]
                 [metosin/reitit "0.7.2"]
                 [com.cognitect/transit-cljs "0.8.280"]]
  :source-paths ["src"
                 "dev"]
  :plugins [[lein-ancient "0.7.0"]])
