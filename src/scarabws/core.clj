(ns scarabws.core
  (:require clojure.string)
  (:require clojure.java.io)
  (:import (java.io File FileReader BufferedReader))
  (:import java.util.Hashtable))


(def ^{:private true} letters [\a \b \c \d \e \f \g \h \i \j \k \l \m \n \o \p \q \r \s \t \u \v \w \x \y \z])

(defn- make-indexfn [indices]
  #(sort-by first compare (into [] (frequencies (map indices %)))))

(defn- make-key-string [key-str i c]
  (apply str key-str (repeat c (letters i))))

(defn- make-info [language freqs words]
  (let [weights (map #(or (freqs %) 0) letters)
        alist (reverse (sort-by last compare (map vector letters weights)))
        indices (zipmap (map first alist) (range 0 26))]
    {:language language 
     :wordcount (count words) 
     :lettercount (reduce + (map count words))
     :counts (map vector (map (comp str first) alist) (map last alist))
     :indices indices}))

(defn- make-table [key level words]
  (let [child-keys (map #(nth % level []) (vals words))
        child-key-set (into #{} child-keys)
        children (apply merge-with conj (zipmap child-key-set (repeat [])) (map hash-map child-keys words))
        own (children [])]
    (into {key (if own (clojure.string/join "," (keys own)) "")}
          (mapcat #(make-table (make-key-string key (first %) (last %)) (inc level) (children %))
                  (filter seq child-key-set)))))

(defn- write-files [path language]
  (let [[freqs words] (#(vector (frequencies %) (clojure.string/split-lines %)) (slurp (str path language ".txt")))
        info (make-info language freqs words)
        indexfn (make-indexfn (:indices info))]
    (spit (str path language ".edn") (pr-str (dissoc info :indexfn)))
    (with-open [out (java.io.BufferedWriter. (java.io.FileWriter. (str path language ".kv")))]
      (doseq [[keystr wordstr] (into (sorted-map) (make-table "" 0 (zipmap words (map indexfn words))))]
        (.write out (str keystr ":" wordstr))
        (.newLine out)))))

(defn- load-language [path language]
  (let [info (read-string (slurp (str path language ".edn")))
        table (java.util.Hashtable.)]
    (with-open [in (java.io.BufferedReader. (java.io.FileReader. (str path language ".kv")))]
      (loop []
        (when-let [kv (.readLine in)]
          (let [kvseq (clojure.string/split kv #"\:")]
            (.put table (or (first kvseq) "") (or (last kvseq) ""))
            (recur)))))
    (assoc info
           :indexfn (make-indexfn (:indices info))
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

(defn- all-match [getfn keymap bounds key-string k jokers]
  (when-let [own (getfn key-string)]
    (apply concat
           (into [] (if (seq own) 
                      (clojure.string/split own #"\,")))
           (for [i (range k (inc (or (first bounds) 25)))
                 :let [maxval (+ jokers (or (keymap i) 0))]
                 c (range 1 (inc maxval))]
             (all-match getfn keymap
                        (if (= i (first bounds)) (rest bounds) bounds)
                        (make-key-string key-string i c)
                        (inc i)
                        (min jokers (- maxval c)))))))

(def ^{:private true} scarab (atom nil))

(defn load-scarab [path]
  (swap! scarab #(or % (into {} (load-all-languages path)))))

(defn language-info 
  ([]
    (into [] (keys (deref scarab))))
  ([language] 
    (dissoc ((deref scarab) language) :indices :indexfn :getfn)))

(defn language-search [language word-query all?]
  (let [dict ((deref scarab) language)
        letters (clojure.string/replace word-query #"_" "")
        jokers (- (count word-query) (count letters))
        key ((:indexfn dict) letters)
        all-matches (all-match (:getfn dict) (into {} key) (if all? [] (map first key)) "" 0 jokers)
        query-pattern (re-pattern (clojure.string/replace word-query #"_" "[a-z]"))]
    (into {:language language
           :query word-query
           :exact-matches (filter #(re-matches query-pattern %) all-matches)}
          (map #(vector (count (first %)) (sort %)) 
               (partition-by count (sort-by count compare (if all? all-matches)))))))