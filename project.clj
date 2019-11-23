(defproject re-state "0.1.0-alpha"
  :description "Statecharts for re-agent"
  :url "https://github.com/glasgolia/re-state"
  :license {:name "MIT"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/clojurescript "1.10.597"]
                 [reagent "0.9.0-rc3"]
                 [glas-state "0.1.0-alpha"]]
  ;:repl-options {:init-ns glasgolia.re-state}
  :plugins [[lein-cljsbuild "1.1.7"]]

  :profiles
  {:dev
            {:dependencies []

             :plugins      []}


   :prod { }
   }


  :cljsbuild {:builds {:app
                       {:source-paths ["src" "env/dev/cljs"]
                        :compiler
                                      {:main          "glasgolia.re-state"
                                       :output-to     "resources/public/js/app.js"
                                       :output-dir    "resources/public/js/out"
                                       :asset-path    "js/out"
                                       :source-map    true
                                       :optimizations :none
                                       :pretty-print  true}

                                      }
                       :release
                       {:source-paths ["src" "env/prod/cljs"]
                        :compiler
                                      {:output-to     "resources/public/js/app.js"
                                       :output-dir    "resources/public/js/release"
                                       :optimizations :advanced
                                       :infer-externs true
                                       :pretty-print  false}}}})



