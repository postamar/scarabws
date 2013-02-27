(ns scarabws.run
  (:use [ring.adapter.jetty :only [run-jetty]])
  (:use [scarabws.core :only [load-scarab]])
  (:require scarabws.web)
  (:gen-class))

(defn -main [& args]
  (load-scarab "dictionnaries/")
  (run-jetty #'scarabws.web/app {:port 8050}))
