(ns scarabws.core
  (:require clojure.string)
  (:require clojure.java.io)
  (:import (java.io File FileReader BufferedReader FileWriter BufferedWriter))
  (:import java.util.Hashtable))

;;; private vars

(def ^{:private true} letters [\a \b \c \d \e \f \g \h \i \j \k \l \m \n \o \p \q \r \s \t \u \v \w \x \y \z])
(def ^{:private true :doc "atom containing all language data"} scarab (atom nil))

;;; key generation

(defn- make-keymapfn [indices]
  #(sort-by first compare (into [] (frequencies (map indices %)))))

(defn- key-append [key c i]
    (apply str key (repeat c (letters i))))

;;; kv-table and statistical info generation

(defn- make-info [language freqs words]
  (let [alist (reverse (sort-by last compare (map #(vector % (or (freqs %) 0)) letters)))]
    {:language language 
     :wordcount (count words) 
     :lettercount (reduce + (map count words))
     :counts (into [] (map (fn [[letter count]] 
                             [(str letter) count]) 
                           alist))
     :indices (zipmap (map first alist) (range 0 26))}))

(defn- make-tree [key level words]
  (let [child-keys (map #(nth % level []) (vals words))
        child-key-set (into #{} child-keys)
        children (apply merge-with conj (zipmap child-key-set (repeat [])) (map hash-map child-keys words))
        own (children [])]
    (into (sorted-map key (if own (clojure.string/join "," (keys own)) ""))
          (mapcat #(make-tree (key-append key (last %) (first %)) (inc level) (children %))
                  (filter seq child-key-set)))))

(defn- write-files [path language]
  (let [[freqs words] (#(vector (frequencies %) (clojure.string/split-lines %)) (slurp (str path language ".txt")))
        info (make-info language freqs words) 
        keymapfn (make-keymapfn (:indices info))]
    (spit (str path language ".edn") (pr-str info))
    (with-open [out (BufferedWriter. (FileWriter. (str path language ".kv")))]
      (doseq [[keystr wordstr] (make-tree "" 0 (zipmap words (map keymapfn words)))]
        (.write out (str keystr ":" wordstr))
        (.newLine out)))))

;;; load language data

(defn- load-language [path language]
  (let [info (read-string (slurp (str path language ".edn")))
        table (Hashtable.)]
    (with-open [in (BufferedReader. (FileReader. (str path language ".kv")))]
      (loop []
        (when-let [kv (.readLine in)]
          (let [kvseq (clojure.string/split kv #"\:")]
            (.put table (or (first kvseq) "") (or (last kvseq) ""))
            (recur)))))
    (assoc info
           :keymapfn (make-keymapfn (:indices info))
           :getfn #(.get table %))))

(defn- split-file-name [file]
  (clojure.string/split (clojure.string/lower-case (.getName file)) #"\."))

(defn- load-all-languages [path]
  (map (fn [[language files]]
         (when (= 1 (count files))
           (write-files path language))
         [language (load-language path language)])
       (group-by (comp first split-file-name)
                 (doall (filter #(and (not (.isDirectory %))
                                      (#{"txt" "edn" "kv"} (last (split-file-name %))))
                                (file-seq (clojure.java.io/file path)))))))

;;; search

(defn- all-match
  "recursively collects all matches"
  [getfn keymap bounds key k jokers]
  (when-let [own (getfn key)]
    (apply concat
           (if (seq own) 
             (clojure.string/split own #"\,"))
           (for [i (range k (inc (or (first bounds) 25)))
                 :let [maxval (+ jokers (or (keymap i) 0))]
                 c (range 1 (inc maxval))]
             (all-match getfn keymap
                        (if (= i (first bounds)) (rest bounds) bounds)
                        (key-append key c i)
                        (inc i)
                        (min jokers (- maxval c)))))))

;;; API

(defn load-scarab 
  "loads language dictionnaries"
  [path]
  (swap! scarab #(or % (into {} (load-all-languages path)))))

(defn language-info 
  "returns dictionnary information"
  ([]
    (into [] (keys @scarab)))
  ([language] 
    (dissoc (@scarab language) :indices :keymapfn :getfn)))

(defn language-search 
  "performs search for words matching a query, exactly or not"
  [language word-query all?]
  (let [dict (@scarab language)
        letters (clojure.string/replace word-query #"_" "")
        jokers (- (count word-query) (count letters))
        keymap (into {} ((:keymapfn dict) letters))
        all-matches (all-match (:getfn dict) keymap (if all? [] (keys keymap)) "" 0 jokers)
        query-pattern (re-pattern (clojure.string/replace word-query #"_" "[a-z]"))]
    (into {:language language
           :query word-query
           :exact-matches (into [] (filter #(re-matches query-pattern %) all-matches))}
          (map #(vector (count (first %)) (sort %)) 
               (partition-by count (sort-by count compare (if all? all-matches)))))))