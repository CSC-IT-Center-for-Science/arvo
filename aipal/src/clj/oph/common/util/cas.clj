(ns oph.common.util.cas
  (:require [clj-http.client :as http]
            [again.core :as again]
            [clojure.tools.logging :as log]
            [aipal.asetukset :refer [asetukset]]
            [oph.common.util.util :refer [oletus-header]]
            [cheshire.core :as cheshire]))

(def kirjautumistila
  (atom {:cs (clj-http.cookies/cookie-store)
         :tgt nil}))

(defn ^:private hae-ticket-granting-url [url user password unsafe-https]
  (let [{:keys [status headers cookies]} (http/post (str url "/v1/tickets")
                                            (oletus-header {:form-params {:username user
                                                                          :password password}
                                                            :insecure? unsafe-https}))]
    (if (= status 201)
      (headers "location")
      (throw (RuntimeException. "CAS-kirjautuminen epäonnistui")))))

(defn ^:private hae-service-ticket [url palvelu-url unsafe-https]
  (let [{:keys [status body]} (http/post url
                                         (oletus-header {:form-params {:service palvelu-url}
                                                        :insecure? unsafe-https}))]
    (if (= status 200)
      body
      (throw (RuntimeException. "Service ticketin pyytäminen CASilta epäonnistui")))))

(defmulti kirjautumisyritys ::again/status)
(defmethod kirjautumisyritys :retry [s]
  (log/warn "RETRY" s)
  (log/info (:cs @kirjautumistila))
  (let [{cas-url :url
         unsafe-https :unsafe-https} (:cas-auth-server @asetukset)
        {palvelu-url :url
         user :user
         password :password} (get @asetukset (-> s ::again/user-context deref :palvelu))
        prequel-url (format "%s/cas/prequel" palvelu-url)
        yritettyuudestaan? (-> s ::again/user-context deref :retried?)
        _ (when (or (not (:tgt @kirjautumistila)) yritettyuudestaan?)
            (swap! kirjautumistila assoc :tgt (hae-ticket-granting-url cas-url user password unsafe-https)))
        ;    Tyhjennä keksit ja hae uusi ST
        service-ticket (hae-service-ticket (:tgt @kirjautumistila) (str palvelu-url "/j_spring_cas_security_check") unsafe-https)]
    (swap! kirjautumistila assoc :cs (clj-http.cookies/cookie-store))
    (log/info (:cs @kirjautumistila))
    ; Lämmittelypyyntö. Ilman tätä muut kuin get-pyynnöt epäonnistuvat (ohjaa kirjautumissivulle)
    (http/get prequel-url (oletus-header {:query-params {"ticket" service-ticket} :cookie-store (:cs @kirjautumistila)})))
  (swap! (::again/user-context s) assoc :retried? true))
(defmethod kirjautumisyritys :success [s]
  (if (-> s ::again/user-context deref :retried?)
    (log/info "SUCCESS after" (::again/attempts s) "attempts" s)
    (log/info "SUCCESS on first attempt" s)))
(defmethod kirjautumisyritys :failure [s]
  (log/error "FAILURE" s))

(defn pyynto [options]
  (let [vastaus (http/request (oletus-header (assoc options :cookie-store (:cs @kirjautumistila))))]
    (log/info (:status vastaus))
    (log/info (:cs @kirjautumistila))
    (if (=(:status vastaus) 302)
      (throw (Exception. "Poikkeus"))
      vastaus)))

(defn request-with-cas-auth [palvelu options]
  (let [{cas-enabled :enabled} (:cas-auth-server @asetukset)]
    (if cas-enabled
      (again/with-retries
       {::again/callback     kirjautumisyritys
        ::again/strategy     [100 100]
        ::again/user-context (atom {:palvelu palvelu})}
;       TODO: testaa virheillä https://httpbin.org/status/500
       (pyynto options))
      (http/request (oletus-header options)))))

(defn get-with-cas-auth
  ([palvelu url]
   (get-with-cas-auth palvelu url {}))
  ([palvelu url options]
   (request-with-cas-auth palvelu (merge options {:method :get
                                                  :url url}))))

(defn post-with-cas-auth
  ([palvelu url]
   (post-with-cas-auth palvelu url {}))
  ([palvelu url options]
   (request-with-cas-auth palvelu (merge options {:method :post
                                                  :url url}))))
