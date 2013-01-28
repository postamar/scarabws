(ns scarabws.run
  (:use ring.adapter.jetty)
  (:require scarabws.web)
  (:gen-class))

(defn -main [& args]
  (scarabws.core/load-scarab)
  (run-jetty #'scarabws.web/app {:port 8050}))
