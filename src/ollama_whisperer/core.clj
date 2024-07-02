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

(comment
  (ns ollama-whisperer.core)
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
