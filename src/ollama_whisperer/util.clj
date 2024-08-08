(ns ollama-whisperer.util)

(defn dot-product
  "Calculates the dot product of two vectors."
  [a b]
  (reduce + (map * a b)))

(defn magnitude
  "Calculates the magnitude (Euclidean norm) of a vector."
  [v]
  (Math/sqrt (reduce + (map #(* % %) v))))

(defn cosine-similarity
  "Calculates the cosine similarity between two vectors."
  [a b]
  (let [dot-prod (dot-product a b)
        mag-a (magnitude a)
        mag-b (magnitude b)]
    (if (or (zero? mag-a) (zero? mag-b))
      0
      (/ dot-prod (* mag-a mag-b)))))

(defn euclidean-distance
  "Calculates the Euclidean distance between two vectors of arbitrary dimensions."
  [vec1 vec2]
  (Math/sqrt
   (reduce + 
           (map (fn [x y] (Math/pow (- x y) 2)) vec1 vec2))))

#_(defn euclidean-distance
    "Calculates the Euclidean distance between two vectors of arbitrary dimensions."
    [vec1 vec2]
    (if (not= (count vec1) (count vec2))
      #_(throw (IllegalArgumentException. "Vectors must be of the same length"))
      (Math/sqrt
       (reduce +
               (map (fn [x y] (Math/pow (- x y) 2)) vec1 vec2)))))

#_(let [[p & embeddings] (->> @(embed default-host "mistral"
                                      ["This is a dog"
                                       "This is a cat"
                                       "This is a dachshund"
                                       "This is a german shepard"
                                       "This is a wheel of cheese"
                                       "This is a rose"])
                              :embeddings)]
    (map #(u/cosine-similarity p %) embeddings))
