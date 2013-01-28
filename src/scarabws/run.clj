(ns scarabws.run
  (:use ring.adapter.jetty)
  (:require scarabws.web)
  (:gen-class))

(defn -main [& args]
  (run-jetty #'scarabws.web/app {:port 8050}))
