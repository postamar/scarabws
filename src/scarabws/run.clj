(ns scarabws.run
  (:use ring.adapter.jetty)
  (:require scarabws.web))

(defn -main [& args]
  (run-jetty #'scarabws.web/app {:port 8050}))
