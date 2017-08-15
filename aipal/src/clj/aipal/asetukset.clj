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

(ns aipal.asetukset
  (:require
    [schema.core :as s]
    [oph.common.infra.asetukset :refer [lue-asetukset]]))

(def asetukset (promise))

(def ^:private Palvelu {:url s/Str
                        :user s/Str
                        :password s/Str})

(def Asetukset
  {:server {:port s/Int
            :base-url s/Str}
   :db {:host s/Str
        :port s/Int
        :name s/Str
        :user s/Str
        :password s/Str
        :maximum-pool-size s/Int
        :minimum-pool-size s/Int}
   :cas-auth-server {:url s/Str
                     :unsafe-https Boolean
                     :enabled Boolean}
   :ldap-auth-server {:host s/Str
                      :port s/Int
                      :user (s/maybe s/Str)
                      :password (s/maybe s/Str)
                      :ssl Boolean}
   :vastaus-base-url s/Str
   :avopfi-shared-secret s/Str
   :organisaatiopalvelu {:url s/Str}
   :koodistopalvelu {:url s/Str}
   :eraajo Boolean
   :development-mode Boolean
   :ominaisuus {s/Keyword Boolean}
   :raportointi-minimivastaajat s/Int
   :logback {:properties-file s/Str}
   :ajastus {:organisaatiopalvelu s/Str
             :kayttooikeuspalvelu s/Str
             :koulutustoimijoiden-tutkinnot s/Str
             :raportointi s/Str
             :tutkinnot s/Str}
   (s/optional-key :basic-auth) {:tunnus s/Str
                                 :salasana s/Str}})

(def oletusasetukset
  {:server {:port 8082
            :base-url ""}
   :db {:host "127.0.0.1"
        :port 3456
        :name "arvo_db"
        :user "aipal_user"
        :password "aipal"
        :maximum-pool-size 15
        :minimum-pool-size 3}
   :cas-auth-server {:url "https://192.168.50.53:8443/cas-server-webapp-3.5.2"
                     :unsafe-https false
                     :enabled false}
   :ldap-auth-server {:host "localhost"
                      :port 10389
                      :user "uid=amkpal,ou=People,dc=opintopolku,dc=fi"
                      :password "salasana"
                      :ssl false}
   :vastaus-base-url "http://192.168.50.1:8083"
   :avopfi-shared-secret "secret"
   :organisaatiopalvelu {:url "https://virkailija.opintopolku.fi/organisaatio-service/rest/organisaatio/"}
   :koodistopalvelu {:url "https://virkailija.opintopolku.fi/koodisto-service/rest/json/"}
   :eraajo false
   :development-mode true ; oletusarvoisesti ei olla kehitysmoodissa. Pitää erikseen kääntää päälle jos tarvitsee kehitysmoodia.
   :ominaisuus {:koulutustoimijan_valtakunnalliset_raportit false}
   :raportointi-minimivastaajat 5
   :logback {:properties-file "resources/logback.xml"}
   :ajastus {:organisaatiopalvelu "5 0 0 * * ?"
             :kayttooikeuspalvelu "0 0 4 * * ?"
             :koulutustoimijoiden-tutkinnot "0 0 5 * * ?"
             :raportointi "0 30 5 * * ?"
             :tutkinnot "0 0 2 * * ?"}})

(defn kehitysmoodi?
  [asetukset]
  (true? (:development-mode asetukset)))

(defn hae-asetukset
  ([alkuasetukset] (lue-asetukset alkuasetukset Asetukset "aipal.properties"))
  ([] (hae-asetukset oletusasetukset)))

(defn service-path [base-url]
  (let [path (drop 3 (clojure.string/split base-url #"/"))]
    (str "/" (clojure.string/join "/" path))))
