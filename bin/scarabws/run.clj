(ns scarabws.run
  (:use ring.adapter.jetty)
  (:require scarabws.web))

(run-jetty #'scarabws.web/app {:port 8050})
