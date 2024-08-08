(ns ollama-whisperer.core
  (:require [aleph.http :as http]
            [clojure.string :as str]
            [jsonista.core :as json]
            [manifold.deferred :as d]
            [camel-snake-kebab.core :as csk]
            [clj-commons.byte-streams :as bs]
            [clojure.spec.alpha :as s])
  (:use [ollama-whisperer.specs]))

(defn- request [host verb path {:keys [stream?] :as opts}]
  (let [url (str host path)
        body (json/write-value-as-string opts
                                         (json/object-mapper
                                          {:encode-key-fn csk/->snake_case_string}))
        opts (merge (assoc opts
                           :body body)
                    (if stream?
                      {:pool (http/connection-pool {:raw-stream? true})}
                      {}))]
    (d/chain (verb url opts)
             :body
             bs/to-string
             #(json/read-value % (json/object-mapper
                                  {:decode-key-fn csk/->kebab-case-keyword})))))

(def default-host "http://localhost:11434/")


;; API Reference used: https://github.com/ollama/ollama/blob/main/docs/api.md

(defn generate 
  ([host model prompt]
   (generate host model prompt {}))
  ([host model prompt opts]
   (let [opts (assoc opts
                     :model model
                     :prompt prompt)]
     (s/assert :api/generate opts)
     (request host http/post "api/generate" opts))))

(defn chat
  ([host model messages]
   (chat host model messages {}))
  ([host model messages opts]
   (let [opts (assoc opts
                     :model model
                     :messages messages)]
     (s/assert :api/chat opts)
     (request host http/post "api/chat" opts))))

(defn embed
  ([host model input]
   (embed host model input {}))
  ([host model input opts]
   (let [opts (assoc opts
                     :model model
                     :input input)]
     (s/assert :api/embed opts)
     (request host http/post "api/embed" opts))))


(comment
  (ns ollama-whisperer.core)

  (require '[ollama-whisperer.util :as u])

  (let [words ["dog"
               "cat"
               "german shepard"
               "dachshund"
               "wiener dog"
               "wolf"
               "poodle"
               "rat"
               "toilet paper"
               "raven"
               "door"
               "apple"
               "mouse"
               "banana"]
        [p & embeddings] (->> @(embed default-host "mistral" #_"mxbai-embed-large"
                                      (map #(str "How close or semantical similar are other terms to: " %) words)
                                      {:options {:temperature 0.0}})
                              :embeddings)]
    (reverse (sort-by :cosine
                      (map (fn [i n]
                             {:cosine (u/cosine-similarity p i)
                              :euclid (u/euclidean-distance p i)
                              :name n}) embeddings (drop 1 words)))))

  (let [words #_["walk" "devour" "kill" "eat" "open" "go" "kiss" "consume" "talk"]
        ["dog"
         "cat"
         "german shepard"
         "dachshund"
         "wiener dog"
         "wolf"
         "poodle"
         "rat"
         "toilet paper"
         "raven"
         "door"
         "apple"
         "mouse"
         "banana"]
        descriptions (-> (apply d/zip (map #(-> (generate default-host "mistral"
                                                          (str "Define this term: " %)
                                                          {:stream false})
                                                (d/chain :response))
                                           words))
                         deref)
        embeddings (->> @(embed default-host "mistral" descriptions)
                        :embeddings)
        ;;first-word (first words)
        first-embedding (first embeddings)]
    (->> (map (fn [word embedding]
                {:cosine (u/cosine-similarity first-embedding embedding)
                 :word word})
              (drop 1 words)
              (drop 1 embeddings))
         (sort-by :cosine)
         (reverse))
    )

  ({:cosine 0.6668495805784235, :word "kiss"}
   {:cosine 0.6655953478295178, :word "talk"}
   {:cosine 0.656328637878942, :word "open"}
   {:cosine 0.6527772637692538, :word "eat"}
   {:cosine 0.6446550922048421, :word "consume"}
   {:cosine 0.5572624582159118, :word "devour"}
   {:cosine 0.5298154924837399, :word "kill"}
   {:cosine 0.445599230205726, :word "go"})

  ({:cosine 0.8080192318824427, :word "poodle"}
   {:cosine 0.7914970488346333, :word "german shepard"}
   {:cosine 0.7665744858739905, :word "cat"}
   {:cosine 0.7567238464239568, :word "wolf"}
   {:cosine 0.7458598578137177, :word "dachshund"}
   {:cosine 0.7420269185748247, :word "wiener dog"}
   {:cosine 0.7287735961807476, :word "rat"}
   {:cosine 0.716195799938144, :word "apple"}
   {:cosine 0.6614460983104878, :word "door"}
   {:cosine 0.656941665480414, :word "banana"}
   {:cosine 0.569966534531085, :word "toilet paper"}
   {:cosine 0.5514438524125816, :word "raven"}
   {:cosine 0.5192320236854663, :word "mouse"})

  (let [words #_["walk" "devour" "kill" "eat" "open" "go" "kiss" "consume" "talk"]
        ["dog"
         "cat"
         "german shepard"
         "dachshund"
         "wiener dog"
         "wolf"
         "poodle"
         "rat"
         "toilet paper"
         "raven"
         "door"
         "apple"
         "mouse"
         "banana"]
        description (-> (generate default-host "mistral"
                                  (str "Define this term: " (first words))
                                  {:stream false})
                        (d/chain :response deref))
        embeddings (->> @(embed default-host "mistral" (cons description (drop 1 words)))
                        :embeddings)
        ;;first-word (first words)
        first-embedding (first embeddings)]
    (->> (map (fn [word embedding]
                {:cosine (u/cosine-similarity first-embedding embedding)
                 :word word})
              (drop 1 words)
              (drop 1 embeddings))
         (sort-by :cosine)
         (reverse))
    )

  ({:cosine 0.6639665295499348, :word "poodle"}
   {:cosine 0.65776329982323, :word "wiener dog"}
   {:cosine 0.6550725165050824, :word "dachshund"}
   {:cosine 0.472514115910195, :word "apple"}
   {:cosine 0.46798706202177803, :word "rat"}
   {:cosine 0.4269323935481669, :word "wolf"}
   {:cosine 0.37885981181551565, :word "banana"}
   {:cosine 0.35472038330693345, :word "door"}
   {:cosine 0.31864205507056453, :word "mouse"}
   {:cosine 0.3011093883625413, :word "german shepard"}
   {:cosine 0.30057482238408934, :word "toilet paper"}
   {:cosine 0.25272050551022396, :word "raven"}
   {:cosine 0.08499437476734462, :word "cat"})


  (let [words #_["walk" "devour" "kill" "eat" "open" "go" "kiss" "consume" "talk"]
        ["dog"
         "cat"
         "german shepard"
         "dachshund"
         "wiener dog"
         "wolf"
         "poodle"
         "rat"
         "toilet paper"
         "raven"
         "door"
         "apple"
         "mouse"
         "banana"]
        descriptions (-> (apply d/zip (map #(-> (generate default-host "mistral"
                                                          (str "Define this term: " %)
                                                          {:stream false})
                                                (d/chain :response))
                                           (drop 1 words)))
                         deref)
        embeddings (->> @(embed default-host "mistral" (cons (first words) descriptions))
                        :embeddings)
        ;;first-word (first words)
        first-embedding (first embeddings)]
    (->> (map (fn [word embedding]
                {:cosine (u/cosine-similarity first-embedding embedding)
                 :word word})
              (drop 1 words)
              (drop 1 embeddings))
         (sort-by :cosine)
         (reverse))
    )

  ({:cosine 0.8204365428356596, :word "german shepard"}
   {:cosine 0.8008587313581033, :word "wolf"}
   {:cosine 0.7760665976525286, :word "wiener dog"}
   {:cosine 0.7014577231595426, :word "poodle"}
   {:cosine 0.6374238726860579, :word "rat"}
   {:cosine 0.6342816291870907, :word "cat"}
   {:cosine 0.6311138758311585, :word "banana"}
   {:cosine 0.620168427841937, :word "apple"}
   {:cosine 0.5998694326289238, :word "raven"}
   {:cosine 0.568341351936068, :word "mouse"}
   {:cosine 0.5327126910215991, :word "toilet paper"}
   {:cosine 0.5285744271003364, :word "door"}
   {:cosine 5.234972865146192E-4, :word "dachshund"})

  ({:cosine 0.030051946636696334, :word "consume"}
   {:cosine 0.0028099349904462186, :word "go"}
   {:cosine -0.0029748331279178053, :word "devour"}
   {:cosine -0.012837156028590334, :word "talk"}
   {:cosine -0.021316197137159577, :word "kiss"}
   {:cosine -0.031824751157542415, :word "eat"}
   {:cosine -0.03454070592842912, :word "kill"}
   {:cosine -0.03608145442239212, :word "open"})

  (let [words #_["walk" "devour" "kill" "eat" "open" "go" "kiss" "consume" "talk"]
        ["dog"
         "cat"
         "german shepard"
         "dachshund"
         "wiener dog"
         "wolf"
         "poodle"
         "rat"
         "toilet paper"
         "raven"
         "door"
         "apple"
         "mouse"
         "banana"]
        descriptions (-> (apply d/zip (map #(-> (generate default-host "mistral"
                                                          (str "List 5 synonymous of " %)
                                                          {:stream false})
                                                (d/chain :response))
                                           words))
                         deref)
        embeddings (->> @(embed default-host "mistral" descriptions)
                        :embeddings)
        ;;first-word (first words)
        first-embedding (first embeddings)]
    (->> (map (fn [word embedding]
                {:cosine (u/cosine-similarity first-embedding embedding)
                 :word word})
              (drop 1 words)
              (drop 1 embeddings))
         (sort-by :cosine)
         (reverse))
    )

  ({:cosine 0.69858859837701, :word "toilet paper"}
   {:cosine 0.6622602307608615, :word "german shepard"}
   {:cosine 0.6524877404780756, :word "rat"}
   {:cosine 0.5659700032084475, :word "raven"}
   {:cosine 0.5372977160884935, :word "wiener dog"}
   {:cosine 0.5355999670473599, :word "wolf"}
   {:cosine 0.5055348361775326, :word "door"}
   {:cosine 0.5027252778362088, :word "apple"}
   {:cosine 0.5002116306304526, :word "dachshund"}
   {:cosine 0.48784600118822313, :word "banana"}
   {:cosine 0.485484935932761, :word "mouse"}
   {:cosine 0.45596983790029344, :word "cat"}
   {:cosine 0.44836078172140714, :word "poodle"})

  ({:cosine 0.8333446211279834, :word "devour"}
   {:cosine 0.746372740896734, :word "consume"}
   {:cosine 0.7417416008983698, :word "open"}
   {:cosine 0.7333700824381553, :word "kill"}
   {:cosine 0.6636030678457479, :word "eat"}
   {:cosine 0.5873450858853151, :word "kiss"}
   {:cosine 0.5536383296270195, :word "go"}
   {:cosine 0.4488849604184596, :word "talk"})

  (let [words #_["walk" "devour" "kill" "eat" "open" "go" "kiss" "consume" "talk"]
        ["dog"
         "cat"
         "german shepard"
         "dachshund"
         "wiener dog"
         "wolf"
         "poodle"
         "rat"
         "toilet paper"
         "raven"
         "door"
         "apple"
         "mouse"
         "banana"]
        descriptions (-> (apply d/zip (map #(-> (generate default-host "mistral"
                                                          (str "Define this term in less than 20 words without mentioning the term itself: " %)
                                                          {:stream false})
                                                (d/chain :response))
                                           words))
                         deref)
        _ (prn descriptions)
        embeddings (->> @(embed default-host "mistral" descriptions)
                        :embeddings)
        ;;first-word (first words)
        first-embedding (first embeddings)]
    (->> (map (fn [word embedding]
                {:cosine (u/cosine-similarity first-embedding embedding)
                 :word word})
              (drop 1 words)
              (drop 1 embeddings))
         (sort-by :cosine)
         (reverse))
    )

  ({:cosine 0.8625275894120386, :word "dachshund"}
   {:cosine 0.7841015741316184, :word "wolf"}
   {:cosine 0.7747719206037823, :word "wiener dog"}
   {:cosine 0.7599121974426875, :word "poodle"}
   {:cosine 0.7283392612226309, :word "german shepard"}
   {:cosine 0.7171287746899668, :word "cat"}
   {:cosine 0.6732653972391737, :word "raven"}
   {:cosine 0.6499467851512707, :word "apple"}
   {:cosine 0.644756128326537, :word "door"}
   {:cosine 0.6305342192583258, :word "banana"}
   {:cosine 0.6086354057677165, :word "mouse"}
   {:cosine 0.5891573249231093, :word "toilet paper"}
   {:cosine 0.5696023833613876, :word "rat"})

  ({:cosine 0.7861623925648629, :word "consume"}
   {:cosine 0.6742159475704973, :word "devour"}
   {:cosine 0.6231034801840338, :word "eat"}
   {:cosine 0.6224442106436027, :word "open"}
   {:cosine 0.6170867284565345, :word "kiss"}
   {:cosine 0.6128364651735095, :word "talk"}
   {:cosine 0.6088522769227336, :word "kill"}
   {:cosine 0.5601219440853727, :word "go"})

  "list 5 synonymous of "
  ({:cosine 0.6929590540068582, :word "go"}
   {:cosine 0.438528890646332, :word "kiss"}
   {:cosine 0.32480259799255096, :word "kill"}
   {:cosine 0.28252683069970824, :word "open"}
   {:cosine 0.2770858124959303, :word "eat"}
   {:cosine 0.24724683213029586, :word "devour"}
   {:cosine 0.15440857448189244, :word "consume"}
   {:cosine 0.11565079335992129, :word "talk"})

  "Define this term in less than 20 words without mentioning the term itself and list 5 synonymous of it: " with "mxbai-embed-large"


  ["poodle" "west highland white terrier" "crow" "dragon" "dog"
   "cat"
   "lizzard"
   "german shepard"
   "dachshund"
   "wiener dog"
   "wolf"
   "poodle"
   "rat"
   "toilet paper"
   "raven"
   "door"
   "apple"
   "mouse"
   "banana"]
  
  
  (-> @(generate default-host "mistral" "Here's a list of verbs: take, open, eat, kiss. Here's a list of items: key, door, apple, dragon. Map following sentence to a edn vector in the format [verb, item] that matches closest to the list of words: Unlock gate. Only return the edn without further text in your reply." {:stream false})
      :response)

  (-> @(generate default-host "mistral" "Here's a list of verbs: take, open, eat, poke. Here's a list of items: key, door, apple, dragon. Map following sentence to a edn vector in the format [verb, item] that matches closest to the list of words: Unlock gate. Only return the edn without further text in your reply." {:stream false})
      :response)

  (-> @(generate default-host "mistral" "Here's a list of verbs: take, open, eat, kiss, poke. Here's a list of items: key, door, apple, dragon. Map following sentence to a edn vector in the format [verb, instrumental-item, target-item] that matches closest to the list of words: Unlock gate. If a verb or item is not explicitly mentioned leave that place in the tuple empty as nil. Only return the edn without further text in your reply." {:stream false})
      :response)

  (-> @(generate default-host "mistral" "Here's a list of verbs: take, open, eat, kiss, poke. Here's a list of items: key, door, apple, dragon. Map following sentence to a edn map with the possible keys: :verb, :with :target that matches closest to the list of words: Unlock gate. Example output for \"eat food with cutlery\": {:verb \"eat\", :target \"apple\", :with \"fork\"}. If a verb or item is not explicitly mentioned leave that place in the tuple empty as nil. Only return the edn without further text in your reply." {:stream false})
      :response)

  (-> @(generate default-host "mistral" "Here's a list of verbs: take, open, eat, kiss, poke. Here's a list of items: key, door, apple, dragon, sword. Map following sentence to a edn map with the possible keys: :verb, :with :target that matches closest to the list of words: Take sword and open door. Example output for \"eat food with cutlery\": {:verb \"eat\", :target \"apple\", :with \"fork\"}. If a verb or item is not explicitly mentioned leave that place in the tuple empty as nil. Only return the edn without further text in your reply. If there are multiple maps put them into a vector" {:stream false})
      :response)

  (-> @(generate default-host "mistral" "Here's a list of verbs: take, open, eat, kiss, poke. Here's a list of items: key, door, apple, dragon, sword. Map following sentence to a edn map with the possible keys: :verb, :with :target that matches closest to the list of words: Open door using key. Example output for \"eat food with cutlery\" is {:verb \"eat\", :target \"apple\", :with \"fork\"}. If a verb or item is not explicitly mentioned leave that place in the tuple empty as nil. Only return the edn without further text in your reply. If there are multiple maps put them into a vector" {:stream false})
      :response))


(comment
  (-> @(chat default-host
             "mistral"
             [{:role "user" :content "who are you?"}
              {:role "assistant" :content "I'm a llama"}
              {:role "user" :content "do you have fur?"}]
             {:stream false})
      :message)

  {:role "assistant",
   :content
   " Yes, I have fur. Llamas are known for their fluffy and soft woolly coats. However, as a text-based entity, I don't actually have fur in reality."}

  {:role "assistant",
   :content
   " Yes, I have soft and woolly fur. It's one of the reasons why I'm so cute!"})
