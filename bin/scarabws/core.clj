(ns scarabws.core
  (:use clojure.java.io)
  (:import java.io.File))

(let [letters [\a \b \c \d \e \f \g \h \i \j \k \l \m \n \o \p \q \r \s \t \u \v \w \x \y \z]]
  (defn- make-indices [weights]
    (into {} (map (fn [[char weight] index]
                    [char index])
                  (sort (fn [[key1 val1] [key2 val2]] (compare val2 val1)) 
                        (map vector letters weights))
                  (range 0 26))))
 
  (defn- read-dictionnary-file [file]
    (let [contents (clojure.string/lower-case (slurp (.getPath file)))
          freqs (frequencies contents)]
      {:language (first (clojure.string/split (clojure.string/lower-case (.getName file)) #"\."))
       :words (re-seq #"\w+" contents)
       :weights (map #(or (freqs %) 0) letters)}))
  
  (defn- make-counts [weights]
    (reverse (sort-by last compare (map vector (map str letters) weights)))))

(defn- make-trie [words]
  (letfn [(wordmapfn [{:keys [indices word]}] 
                     [(first indices) {:indices (rest indices) :word word}])
          (partfn [part] 
                  (let [key (first (first part))
                        value (map last part)]
                    (if (nil? key)
                      [:words (map :word value)]
                      [key (make-trie value)])))]
    (into {} (if (seq words)
               (map partfn (partition-by first (sort-by first compare (map wordmapfn words))))))))
    
(defn- match-in-trie [node indices jokers current]
  (if (seq node)
    (if (empty? indices)
      (if (= current 26)
        (:words node)
        (concat (match-in-trie node [] jokers (+ 1 current))
                (mapcat #(match-in-trie (node (vector current (+ 1 %))) [] (- jokers 1 %) (+ 1 current)) (range 0 jokers))))        
      (let [[[i n] & remaining] indices]
        (if (= i current)
          (concat (match-in-trie node remaining jokers (+ 1 i))
                  (mapcat #(match-in-trie (node (vector i %)) remaining jokers (+ 1 i)) (range 1 (+ n 1)))
                  (mapcat #(match-in-trie (node (vector i (+ n 1 %))) remaining (- jokers 1 %) (+ 1 i)) (range 0 jokers)))
          (concat (match-in-trie node indices jokers (+ 1 current))
                  (mapcat #(match-in-trie (node (vector current (+ 1 %))) indices (- jokers 1 %) (+ 1 current)) (range 0 jokers))))))))
        
(defn- make-queryfn [language words weights]
  (let [indices (make-indices weights)
        indexfn (fn [word] (sort-by first compare (into [] (frequencies (map #(indices %) word)))))
        trie (make-trie (map #(hash-map :indices (indexfn %) :word %) words))]
    (fn [word-query all?]
      (let [letters (clojure.string/replace word-query #"_" "")
            query-pattern (re-pattern (clojure.string/replace word-query #"_" "[a-z]"))
            jokers (- (count word-query) (count letters))
            all-matches (into [] (match-in-trie trie (indexfn letters) jokers 0))]
        (into {:language language
               :query word-query
               :exact-matches (filter #(re-matches query-pattern %) all-matches)}
              (if (and all? (seq all-matches))
                (map #(vector (count (first %)) %)
                     (map sort (partition-by count (sort-by count compare all-matches))))))))))

(def scarab (atom {}))

(defn load-scarab []
  (swap! scarab (into {} (map (fn [{:keys [language words weights]}]
                                [language {:info {:language language :size (count words) :counts (make-counts weights)}
                                           :queryfn (make-queryfn language words weights)}])
                              (doall (map read-dictionnary-file (filter #(not (.isDirectory %)) (file-seq (clojure.java.io/file "dictionnaries/")))))))))

(defn get-languages []
  (into [] (map first (deref scarab))))

(defn get-language [language]
  ((deref scarab) language))
    
  
    
   
