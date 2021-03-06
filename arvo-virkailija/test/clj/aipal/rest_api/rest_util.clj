(ns arvo.rest-api.rest-util
  (:require [peridot.core :as peridot]
            [clj-time.core :as time]
            [cheshire.core :as cheshire]
            [oph.common.infra.i18n :as i18n]
            [oph.common.infra.common-audit-log-test :as common-audit-log-test]
            [arvo.palvelin :as palvelin]
            [arvo.integraatio.sql.korma :as korma]
            [arvo.infra.kayttaja.vaihto :refer [with-kayttaja]]
            [arvo.infra.kayttaja.vakiot :refer [default-test-user-uid]]
            [arvo.sql.test-util :refer :all]
            [arvo.sql.test-data-util :refer :all]
            [buddy.sign.jws :as jws]
            [arvo.config :refer [env]]))

(defn with-auth-user [f]
  (with-kayttaja default-test-user-uid nil nil
    (binding [i18n/*locale* testi-locale]
      (f))))

(defn mock-request-uid
  ([app url method uid params]
   (peridot/request app url
                    :request-method method
                    :headers {"uid" uid}
                    :params params))
  ([app url method uid params body]
   (peridot/request app url
                    :request-method method
                    :headers {"uid" uid}
                    :content-type "application/json"
                    :body (cheshire/generate-string body)
                    :params params)))

(defn mock-request-salaisuus
  ([app url method auth-header params body
    (peridot/request app url
                    :request-method method
                    :headers {:Authorization auth-header}
                    :content-type "application/json"
                    :body (cheshire/generate-string body)
                    :params params)]))

(defn session []
  (let [asetukset (-> oletusasetukset
                    (assoc-in [:cas-auth-server :enabled] false)
                    (assoc-in [:server :base-url] "http://localhost:8080")
                    (assoc :development-mode true
                           :basic-auth {:tunnus "tunnus"
                                        :salasana "salasana"}))]
    (alusta-korma! asetukset)
    (-> (palvelin/app asetukset)
      (peridot/session :cookie-jar {"localhost" {"XSRF-TOKEN" {:raw "XSRF-TOKEN=token", :domain "localhost", :path "/", :value "token"}
                                                 "ring-session" {:raw (str "ring-session=" (:session common-audit-log-test/test-request-meta)), :domain "localhost", :path "/", :value (:session common-audit-log-test/test-request-meta)}}})

      (peridot/header "uid" testikayttaja-uid)
      (peridot/header "x-xsrf-token" "token")
      (peridot/header "user-agent" (:user-agent common-audit-log-test/test-request-meta))
      (peridot/header "X-Forwarded-For" "192.168.50.1")
      (peridot/content-type "application/json; charset=utf-8"))))

;;TODO : Remove xsrf-token from here
(defn session-no-token []
  (let [asetukset (-> env
                    (assoc-in [:cas-auth-server :enabled] false)
                    (assoc :development-mode true))]
    (alusta-korma! asetukset)
    (-> (peridot/session (palvelin/app asetukset)
                :cookie-jar {"localhost" {"XSRF-TOKEN" {:raw "XSRF-TOKEN=token", :domain "localhost", :path "/", :value "token"}}})
      (peridot/content-type "application/json"))))

(defn rest-kutsu
  "Tekee yksinkertaisen simuloidun rest-kutsun. Peridot-sessio suljetaan
lopuksi. Soveltuuyksinkertaisiin testitapauksiin."
  ([url method params]
   (-> (session)
       (mock-request-uid url method "T-1001" params)
       :response))
  ([url method params body]
   (-> (session)
       (mock-request-uid url method "T-1001" params body)
       :response)))

(defn body-json [response]
  (if (string? (:body response))
    (cheshire/parse-string (:body response) true)
    (cheshire/parse-string (slurp (:body response) :encoding "UTF-8") true)))
