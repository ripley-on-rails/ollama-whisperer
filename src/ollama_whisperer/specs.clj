(ns ollama-whisperer.specs
  (:require [clojure.spec.alpha :as s]))

(s/check-asserts true)

(s/def ::string-or-coll-iof-strings
  (s/or :string string?
        :coll-of-strings (s/coll-of string?)))

(s/def :options/model string?)
(s/def :options/prompt string?)
(s/def :options/images (s/coll-of string?))
(s/def :options/format #{"json"})
(s/def :options/options (s/map-of keyword? any?))
(s/def :options/system string?)
(s/def :options/template string?)
(s/def :options/context (s/coll-of int?))
(s/def :options/stream boolean?)
(s/def :options/truncate boolean?)
(s/def :options/input ::string-or-coll-iof-strings)
(s/def :options/raw boolean?)
(s/def :options/keep-alive string?)
(s/def :options/role #{"system" "user" "assistant"})
(s/def :options/content string?)
(s/def :options/message (s/keys :req-un [:options/role
                                         :options/content]
                                :opt-un [:options/images]))
(s/def :options/messages (s/coll-of :options/message))

(s/def :api/generate
  (s/keys :req-un [:options/model
                   :options/prompt]
          :opt-un [:options/images
                   :options/format
                   :options/options
                   :options/system
                   :options/template
                   :options/context
                   :options/stream
                   :options/raw
                   :options/keep-alive]))

(s/def :api/chat
  (s/keys :req-un [:options/model
                   :options/messages]
          :opt-un [:options/format
                   :options/options
                   :options/stream
                   :options/keep-alive]))

(s/def :api/embed
  (s/keys :req-un [:options/model
                   :options/input]
          :opt-un [:options/truncate
                   :options/options
                   :options/keep-alive]))

