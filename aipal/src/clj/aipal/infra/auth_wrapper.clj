;; Copyright (c) 2013 The Finnish National Board of Education - Opetushallitus
;;
;; This program is free software:  Licensed under the EUPL, Version 1.1 or - as
;; soon as they will be approved by the European Commission - subsequent versions
;; of the EUPL (the "Licence");
;;
;; You may not use this work except in compliance with the Licence.
;; You may obtain a copy of the Licence at: http://www.osor.eu/eupl/
;;
;; This program is distributed in the hope that it will be useful,
;; but WITHOUT ANY WARRANTY; without even the implied warranty of
;; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
;; European Union Public Licence for more details.

(ns aipal.infra.auth-wrapper
  "Authentication middleware. Sitoo käyttäjätunnuksen requestille ennen kuin tietokantayhteys on avattu."
  (:require [ring.util.response :refer [redirect]]
            [compojure.core :as c]
            [oph.korma.korma-auth :as ka]
            [aipal.toimiala.kayttajaoikeudet :as ko]
            [aipal.arkisto.kayttaja :as kayttaja-arkisto]
            [aipal.arkisto.kayttajaoikeus :as kayttajaoikeus-arkisto]
            [clojure.tools.logging :as log]))

(defn get-userid-from-request
  "cas filter laittaa :username nimiseen propertyyn käyttäjätunnuksen"
  [request]
  {:post [(not (nil? %))]}
  (:username request))

(defn with-user [userid impersonoitu-oid f]
  (binding [ka/*current-user-uid* userid
            ka/*current-user-oid* (promise)]
    (let [kayttaja (kayttaja-arkisto/hae-uid userid)]
      (kayttajaoikeus-arkisto/varmista-autentikointi!)
      (binding [ka/*effective-user-oid* (or impersonoitu-oid (:oid kayttaja))]
        (let [impersonoitu-kayttaja (kayttaja-arkisto/hae impersonoitu-oid)
              oikeudet (kayttajaoikeus-arkisto/hae-oikeudet ka/*effective-user-oid*)
              kayttajatiedot {:kayttajan_nimi (str (:etunimi kayttaja) " " (:sukunimi kayttaja))}
              auth-map (assoc kayttajatiedot 
                         :roolit (:roolit oikeudet)
                         :impersonoitu_kayttaja (str (:etunimi impersonoitu-kayttaja) " " (:sukunimi impersonoitu-kayttaja)))]
      (log/info "käyttäjä autentikoitu " auth-map )
      (binding [ko/*current-user-authmap* auth-map]
        (deliver ka/*current-user-oid* (:oid kayttaja))
        (f)))))))

(defn wrap-sessionuser [ring-handler]
  (fn [request]
    (let [userid (get-userid-from-request request)
          impersonoitu-oid (get-in request [:session :impersonoitu-oid])
          _ (log/debug "userid set to " userid ", impersonated oid " impersonoitu-oid)]
      (with-user userid impersonoitu-oid 
        #(ring-handler request)))))
