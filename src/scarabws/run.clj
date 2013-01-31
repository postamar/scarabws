(ns scarabws.run
  (:use ring.adapter.jetty)
  (:require scarabws.core)
  (:require scarabws.web)
  (:gen-class))

(defn -main [& args]
  (swap! scarabws.core/scarab scarabws.core/load-scarab "dictionnaries/")
  (run-jetty #'scarabws.web/app {:port 8050}))