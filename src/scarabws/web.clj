(ns scarabws.web
  (:use [scarabws.core :only [language-info language-search]])
  (:use compojure.core)
  (:use [ring.middleware.json :only [wrap-json-response]])
  (:use [ring.util.response :only [not-found response]]))

(defn- validate-language [language]
  (and (not (language-info language))
       (not-found {:error (str "invalid language: " language)})))

(defn- validate-query [word-query]
  (and (or (not (re-matches #"^[a-z_]{2,15}$" word-query))
           (re-matches #"(?:[a-z]*_){3,}[a-z_]*" word-query))       
       (not-found {:error (str "invalid word query: " word-query)})))

(defroutes handlers 
  (GET "/" 
       []
       (response (language-info)))
  (GET "/:language" 
       [language]
       (or (validate-language language)
           (response (language-info language))))
  (GET "/:language/:word-query"
       {{language :language word-query :word-query} :params query-string :query-string}
       (or (validate-language language)
           (validate-query word-query)
           (response (language-search language word-query (not= query-string "exact")))))
  (ANY "*" 
       [] 
       (not-found {:error "404 - not found"})))

(def app
  (wrap-json-response handlers))
