(ns scarabws.web
  (:require scarabws.core)
  (:use compojure.core)
  (:use ring.middleware.json)
  (:use ring.util.response))

(defmacro with-language [language & body]
  `(if (not (contains? (scarabws.core/get-languages) ~language))
     (not-found {:error (str "invalid language: " ~language)})
     ~@body))

(defmacro with-language-and-word-query [language word-query & body]
  `(with-language ~language
     (if (or (not (re-matches #"^[a-z_]{2,15}$"     ~word-query))
             (re-matches #"(?:[a-z]*_){3,}[a-z_]*"  ~word-query))       
       (not-found {:error (str "invalid word query: " ~word-query)})
       ~@body)))

(defroutes handlers 
  (GET "/" 
       []
       (response (into [] (scarabws.core/get-languages))))
  (GET "/:language" 
       [language]
       (with-language language 
         (response (scarabws.core/get-language language))))
  (GET "/:language/:word-query"
       {{language :language word-query :word-query} :params query-string :query-string}
       (with-language-and-word-query language word-query
         (response (scarabws.core/search-language language word-query (not= query-string "exact")))))
  (ANY "*" 
       [] 
       (not-found {:error "404 - not found"})))

(def app
  (wrap-json-response handlers))
