(defproject realm "0.1.1"
  :description "Elm architecture for Reagent components"
  :url "https://github.com/jiangts/realm"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.10.439"]
                 [re-frame "0.10.5"]
                 [reagent "0.8.1"]
                 [devcards "0.2.6"]]
  :plugins [[lein-cljsbuild "1.1.3"]
            [lein-doo "0.1.6"]
            [lein-figwheel "0.5.18"]]
  :clean-targets ^{:protect false} ["resources/js/"]
  :doo {:paths {:rhino "lein run -m org.mozilla.javascript.tools.shell.Main"}}
  :aliases {"test" ["with-profile" "test" "doo" "chrome" "test" "once"]}
  :profiles
  {:test {:dependencies [[org.mozilla/rhino "1.7.7"]]
          :cljsbuild
          {:builds
           {:test
            {:source-paths ["src" "test" "dev"]
             :compiler {:output-to "target/main.js"
                        :output-dir "target"
                        :main realm.doo-runner
                        :optimizations :simple}}}}}}
  :figwheel {:http-server-root "."
             :nrepl-port 7003
             :nrepl-middleware [cider.piggieback/wrap-cljs-repl]
             :server-port 3003
             :css-dirs ["resources/css"]}

  :cljsbuild {:builds {:devcards
                       {:source-paths ["src" "test"]
                        :figwheel {:devcards true}
                        :compiler {:main realm.test-runner
                                   :optimizations :none
                                   :output-to "resources/js/main.js"
                                   :output-dir "resources/js"
                                   :asset-path "js"}}}})
