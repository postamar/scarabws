(ns scarabws.core
  (:use clojure.java.io)
  (:import java.io.File))

(def letters [\a \b \c \d \e \f \g \h \i \j \k \l \m \n \o \p \q \r \s \t \u \v \w \x \y \z])

(defn- match-in-trie [node indices jokers current]
  (if (seq node)
    (if (empty? indices)
      (if (= current 26)
        (node [])
        (concat (match-in-trie node [] jokers (+ 1 current))
                (mapcat #(match-in-trie (node (vector current (+ 1 %))) [] (- jokers 1 %) (+ 1 current)) (range 0 jokers))))        
      (let [[[i n] & remaining] indices]
        (if (= i current)
          (concat (match-in-trie node remaining jokers (+ 1 i))
                  (mapcat #(match-in-trie (node (vector i %)) remaining jokers (+ 1 i)) (range 1 (+ n 1)))
                  (mapcat #(match-in-trie (node (vector i (+ n 1 %))) remaining (- jokers 1 %) (+ 1 i)) (range 0 jokers)))
          (concat (match-in-trie node indices jokers (+ 1 current))
                  (mapcat #(match-in-trie (node (vector current (+ 1 %))) indices (- jokers 1 %) (+ 1 current)) (range 0 jokers))))))))

(defn- make-trie [level pairs]
  (let [keys (map #(nth (first %) level []) pairs)]    
    (into (sorted-map)
          (map (fn [[key val]]
                 [key (if (= [] key)                          
                        (map last val)
                        (make-trie (inc level) val))])
               (apply merge-with conj (into {} (map vector (into #{} keys) (repeat []))) (map hash-map keys pairs))))))

(defn- make-indexfn [indices]
  #(sort-by first compare (into [] (frequencies (map indices %)))))

(defn- make-dictionnary [language contents]
  (let [words (re-seq #"\w+" contents)
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
     :trie (make-trie 0 (map #(vector (indexfn %) %) words))}))

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
                                 dict (make-dictionnary language (clojure.string/lower-case (slurp txt-file-path)))]
                             (spit clj-file-path (pr-str dict))
                             [language dict]))
                         (filter (comp nil? clj first) 
                                 (map pop (filter #(= "txt" (peek %)) files))))))))

(defn load-scarab [scarab-current-value path]
  (if (not (nil? scarab-current-value))
    scarab-current-value
    (load-dictionnary-files path)))

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
        all-matches (into [] (match-in-trie (dict :trie) ((make-indexfn (dict :indices)) letters) jokers 0))
        query-pattern (re-pattern (clojure.string/replace word-query #"_" "[a-z]"))]
    (into {:language language
           :query word-query
           :exact-matches (filter #(re-matches query-pattern %) all-matches)}
          (map #(vector (count (first %)) %)
               (map sort (partition-by count (sort-by count compare (when all? all-matches))))))))
