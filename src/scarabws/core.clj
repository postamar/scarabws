(ns scarabws.core
  (:use clojure.java.io)
  (:import java.io.File))

(def letters [\a \b \c \d \e \f \g \h \i \j \k \l \m \n \o \p \q \r \s \t \u \v \w \x \y \z])

(defn- make-indexfn [indices]
  #(sort-by first compare (into [] (frequencies (map indices %)))))

(defn- make-key-string [key [i c]]
  (apply str key (repeat c (letters i))))

(defn- make-table [key level pairs]
  (let [child-keys (map #(nth (first %) level []) pairs)
        child-key-set (into #{} child-keys)
        children (apply merge-with conj (zipmap child-key-set (repeat [])) (map hash-map child-keys pairs))
        own (children [])]
    (into {key (if own (clojure.string/join "," (map last own)) "")}
          (mapcat #(make-table (make-key-string key %) (inc level) (children %))
                  (filter seq child-key-set)))))

(defn- match [table keymap key-string k jokers]
  (when-let [own (table key-string)]
    (apply concat
           (into [] (if (seq own) 
                      (clojure.string/split own #"\,")))
           (for [i (range k 26)
                 :let [maxval (+ jokers (or (keymap i) 0))]
                 c (range 1 (inc maxval))]
             (match table keymap
                    (make-key-string key-string [i c])
                    (inc i)
                    (min jokers (- maxval c)))))))
  
(defn- make-dictionnary [language contents]
  (let [words (clojure.string/split-lines contents)
        freqs (frequencies contents)
        weights (map #(or (freqs %) 0) letters)
        alist (reverse (sort-by last compare (map vector letters weights)))
        indices (zipmap (map first alist) (range 0 26))
        indexfn (make-indexfn indices)]
    {:language language 
     :wordcount (count words) 
     :lettercount (reduce + (map count words))
     :counts (map vector (map (comp str first) alist) (map last alist))
     :indices indices
     :table (into (sorted-map) (make-table [] 0 (map #(vector (indexfn %) %) words)))}))

(def scarab (atom nil))

(defn- split-dictionnary-path [file]
  (let [parts (clojure.string/split (clojure.string/lower-case (.getName file)) #"\.")]
    [(clojure.string/join "." (butlast parts)) (.getPath file) (last parts)]))
  
(defn- load-dictionnary-files [path]
  (let [files (doall (map split-dictionnary-path (filter #(not (.isDirectory %)) (file-seq (clojure.java.io/file path)))))
        clj (into {} (map pop (filter #(= "clj" (peek %)) files)))]
    (merge (into {} (map (fn [[language clj-file-path]]
                           [language (read-string (slurp clj-file-path))])
                         clj))
           (into {} (map (fn [[language txt-file-path]]
                           (let [clj-file-path (str path language ".clj")
                                 dict (make-dictionnary language (slurp txt-file-path))]
                             (spit clj-file-path (pr-str dict))
                             [language dict]))
                         (filter (comp nil? clj first) 
                                 (map pop (filter #(= "txt" (peek %)) files))))))))

(defn load-scarab [scarab-current-value path]
  (if (nil? scarab-current-value)
    (load-dictionnary-files path)
    scarab-current-value))

(defn get-languages []
  (into #{} (map first (deref scarab))))

(defn get-language [language]
  (let [dict ((deref scarab) language)
        keys [:language :wordcount :lettercount :counts]]
    (zipmap keys (map dict keys))))

(defn search-language [language word-query all?]
  (let [dict ((deref scarab) language)
        letters (clojure.string/replace word-query #"_" "")
        jokers (- (count word-query) (count letters))
        all-matches (match (dict :table) 
                           (into {} ((make-indexfn (dict :indices)) letters)) 
                           ""
                           0
                           (- (count word-query) (count letters)))
        query-pattern (re-pattern (clojure.string/replace word-query #"_" "[a-z]"))]
    (into {:language language
           :query word-query
           :exact-matches (filter #(re-matches query-pattern %) all-matches)}
          (map #(vector (count (first %)) (sort %)) 
               (partition-by count (sort-by count compare (if all? all-matches)))))))