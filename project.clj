(defproject scarabws "1.1.2"
  :description "Scarab Web Service"
  :url "http://scarabws.mariusposta.info"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [ring "1.1.8"]
                 [ring/ring-json "0.1.2"]
                 [compojure "1.1.5"]]
  :main scarabws.run)

