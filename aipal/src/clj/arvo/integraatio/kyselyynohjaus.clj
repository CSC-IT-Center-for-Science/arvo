(ns arvo.integraatio.kyselyynohjaus
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [clojure.walk :refer [keywordize-keys]]
            [arvo.config :refer [env]]))

(defn get-opiskeluoikeus-data [oppilaitokset]
  (let [{url :url
         user :user
         password :password }(:kyselyynohjaus env)]
    (-> (http/post url {:basic-auth [user password]
                        :form-params {:oppilaitokset oppilaitokset}
                        :insecure? (:development-mode env)
                        :content-type :json})

        :body
        json/parse-string
        keywordize-keys)))
