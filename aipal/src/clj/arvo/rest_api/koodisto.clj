(ns arvo.rest-api.koodisto
  (:require [buddy.auth.middleware :refer (wrap-authentication)]
            [compojure.api.sweet :refer :all]
            [buddy.auth.backends.token :refer (jws-backend)]
            [schema.core :as s]
            [oph.common.util.http-util :refer [response-or-404]]
            [arvo.db.core :refer [*db*] :as db]))

(defn get-shared-secret [asetukset]
  (get-in asetukset [:avopfi-shared-secret]))

(defn auth-backend [asetukset] (jws-backend {:secret (get-shared-secret asetukset) :token-name "Bearer"}))

(defroutes reitit
  (GET "/oppilaitos/:oppilaitosnumero" []
    :path-params [oppilaitosnumero :- String]
    (let [oppilaitos (db/oppilaitos {:oppilaitoskoodi oppilaitosnumero})]
      (response-or-404 (into {}
                         (filter second {"fi" (:nimi_fi oppilaitos)
                                         "sv" (:nimi_sv oppilaitos)
                                         "en" (:nimi_en oppilaitos)})))))

  ;TODO: move auth to new system, check auth
  (GET "/koulutus/:koulutuskoodi" []
    :path-params [koulutuskoodi :- String]
    (let [tutkinto (db/tutkinto {:tutkintotunnus koulutuskoodi})]
      (response-or-404 (into {}
                             (filter second {"fi" (:nimi_fi tutkinto)
                                             "sv" (:nimi_sv tutkinto)
                                             "en" (:nimi_en tutkinto)}))))))

(defn koodisto-reitit [asetukset]
  (wrap-authentication reitit (auth-backend asetukset)))
