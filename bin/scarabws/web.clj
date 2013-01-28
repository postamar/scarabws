(ns scarabws.web
  (:require scarabws.core)
  (:use compojure.core)
  (:use ring.middleware.json)
  (:use ring.util.response))

(defmacro with-language [language & body]
  `(if (not (scarabws.core/get-language ~language))
     (not-found {:error (str "invalid language: " ~language)})
     ~@body))

(defmacro with-language-and-word-query [language word-query & body]
  `(with-language ~language
     (if (or (not (re-matches #"^[a-z_]{2,15}$"     ~word-query))
             (re-matches #"(?:[a-z]*_){3,}[a-z_]*" ~word-query))       
       (not-found {:error (str "invalid word query: " ~word-query)})
       ~@body)))

(defroutes handlers 
  (GET "/" 
       []
       (response (scarabws.core/get-languages)))
  (GET "/:language" 
       [language]
       (with-language language 
         (response (:info (scarabws.core/get-language language)))))
  (GET "/:language/:word-query"
       {{language :language word-query :word-query} :params query-string :query-string}
       (with-language-and-word-query language word-query
         (response ((:queryfn (scarabws.core/get-language language)) word-query (not= query-string "exact")))))
  (ANY "*" 
       [] 
       (not-found {:error "404 - not found"})))

(def app
  (wrap-json-response handlers))
