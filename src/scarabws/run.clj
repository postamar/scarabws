(ns scarabws.run
  (:use ring.adapter.jetty)
  (:require scarabws.web))

(let [port (Integer/parseInt (get (System/getenv) "PORT" "8088"))]
  (run-jetty #'scarabws.web/app {:port port}))
