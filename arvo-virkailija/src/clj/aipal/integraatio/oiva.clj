(ns aipal.integraatio.oiva
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [clojure.walk :refer [keywordize-keys]]
            [arvo.config :refer [env]]))

(defn hae-koulutustoimijoiden-tutkinnot []
  (let [{url :url
         user :user
         password :password }(:oiva env)]
    (-> (http/get url
                  {:basic-auth [user password]})
        :body
        json/parse-string
        keywordize-keys)))
