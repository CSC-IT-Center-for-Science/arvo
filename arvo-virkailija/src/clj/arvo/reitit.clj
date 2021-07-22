(ns arvo.reitit
  (:require [clojure.pprint :refer [pprint]]

            [cheshire.core :as cheshire]
            [compojure.api.exception :as ex]
            [compojure.api.sweet :refer [api context swagger-routes GET]]
            [compojure.route :as r]
            [stencil.core :as s]

            [oph.common.infra.csrf-token :refer [aseta-csrf-token wrap-tarkasta-csrf-token]]
            [arvo.basic-auth :refer [wrap-basic-authentication]]
            arvo.rest-api.i18n
            arvo.rest-api.kysely
            arvo.rest-api.kyselykerta
            arvo.rest-api.kyselypohja
            arvo.rest-api.kysymysryhma
            arvo.rest-api.ohje
            arvo.rest-api.oppilaitos
            arvo.rest-api.toimipiste
            arvo.rest-api.raportti.kysely
            arvo.rest-api.raportti.kyselykerta
            arvo.rest-api.raportti.valtakunnallinen
            arvo.rest_api.js-log
            arvo.rest-api.vastaajatunnus
            arvo.rest-api.automaattitunnus
            arvo.rest-api.henkilo
            arvo.rest-api.uraseuranta
            arvo.rest-api.koodisto
            arvo.rest-api.kayttaja
            arvo.rest-api.tutkinto
            arvo.rest-api.tutkintotyyppi
            arvo.rest-api.koulutustoimija
            arvo.rest-api.tiedote
            arvo.rest-api.export
            arvo.rest-api.move
            arvo.rest-api.admin
            [compojure.api.middleware :as mw]
            [arvo.auth.api :refer [wrap-authentication]]
            [arvo.infra.kayttaja :refer [*kayttaja*]]
            [arvo.util :refer [service-path]]
            [arvo.config :refer [project-version]]))

(defn reitit [asetukset]
  (api
    {:exceptions {:handlers {:schema.core/error ex/schema-error-handler}}}
    (swagger-routes
        {:ui "/api-docs"
         :spec "/swagger.json"
         :data {:info {:title "Arvo API"
                       :version "1.0.0"
                       :description "Arvon rajapinnat.
                       Glossary: https://wiki.eduuni.fi/display/CscArvo/Glossary"}
                :basePath (str (service-path (get-in asetukset [:server :base-url] "/api")))
                :tags [{:name "export" :description "Kyselytietojen siirtorajapinta"}]}})
    (context "" [] :no-doc true
             (GET "/" [] {:status 200
                          :headers {"Content-type" "text/html; charset=utf-8"
                                    "Set-cookie" (aseta-csrf-token (-> asetukset :server :base-url service-path))}
                          :body (s/render-file "public/app/index.html"
                                               (merge {:base-url (-> asetukset :server :base-url)
                                                       :vastaus-base-url (-> asetukset :vastaus-base-url)
                                                       :current-user (:nimi *kayttaja*)
                                                       :project-version @project-version
                                                       :development-mode (pr-str (:development-mode asetukset))
                                                       :ominaisuus (cheshire/generate-string (:ominaisuus asetukset))}
                                                     (when-let [cas-url (-> asetukset :cas-auth-server :url)]
                                                       {:logout-url (str cas-url "/logout")})))}))
    (context "/api/jslog" [] :no-doc true :middleware [wrap-tarkasta-csrf-token] arvo.rest_api.js-log/reitit)
    (context "/api/i18n" [] :no-doc true arvo.rest-api.i18n/reitit)
    (context "/api/kyselykerta" [] :no-doc true :tags ["kyselykerta"] :middleware [wrap-tarkasta-csrf-token] arvo.rest-api.kyselykerta/reitit)
    (context "/api/kyselypohja" [] :no-doc true :tags ["kyselypohja"] arvo.rest-api.kyselypohja/tiedosto-reitit)
    (context "/api/kyselypohja" [] :no-doc true :tags ["kyselypohja"] :middleware [wrap-tarkasta-csrf-token] arvo.rest-api.kyselypohja/reitit)
    (context "/api/ohje" [] :no-doc true :tags ["ohje"] :middleware [wrap-tarkasta-csrf-token] arvo.rest-api.ohje/reitit)
    (context "/api/oppilaitos" [] :no-doc true :tags ["oppilaitos"] :middleware [wrap-tarkasta-csrf-token] arvo.rest-api.oppilaitos/reitit)
    (context "/api/toimipiste" [] :no-doc true :tags ["toimipiste"] :middleware [wrap-tarkasta-csrf-token] arvo.rest-api.toimipiste/reitit)
    (context "/api/raportti/kysely" [] :no-doc true :tags ["raportti"] (arvo.rest-api.raportti.kysely/csv-reitit asetukset))
    (context "/api/raportti/kysely" [] :no-doc true :tags ["raportti"] :middleware [wrap-tarkasta-csrf-token] (arvo.rest-api.raportti.kysely/reitit asetukset))
    (context "/api/raportti/kyselykerta" [] :no-doc true :tags ["raportti"] (arvo.rest-api.raportti.kyselykerta/csv-reitit asetukset))
    (context "/api/raportti/kyselykerta" [] :no-doc true :tags ["raportti"] :middleware [wrap-tarkasta-csrf-token] (arvo.rest-api.raportti.kyselykerta/reitit asetukset))
    (context "/api/kysely" [] :no-doc true :tags ["kysely"] :middleware [wrap-tarkasta-csrf-token] arvo.rest-api.kysely/reitit)
    (context "/api/kysymysryhma" [] :no-doc true :tags ["kysymysryhma"]:middleware [wrap-tarkasta-csrf-token] arvo.rest-api.kysymysryhma/reitit)
    (context "/api/vastaajatunnus" [] :no-doc true :tags ["vastaajatunnus"] :middleware [wrap-tarkasta-csrf-token] arvo.rest-api.vastaajatunnus/reitit)
    (context "/api/kayttaja" [] :no-doc true :tags ["kayttaja"] :middleware [wrap-tarkasta-csrf-token] arvo.rest-api.kayttaja/reitit)
    (context "/api/tutkinto" [] :no-doc true :tags ["tutkinto"] :middleware [wrap-tarkasta-csrf-token] arvo.rest-api.tutkinto/reitit)
    (context "/api/tutkintotyyppi" [] :no-doc true :tags ["tutkinto"]  :middleware [wrap-tarkasta-csrf-token] arvo.rest-api.tutkintotyyppi/reitit)
    (context "/api/koulutustoimija" [] :no-doc true :tags ["koulutustoimija"] :middleware [wrap-tarkasta-csrf-token] arvo.rest-api.koulutustoimija/reitit)
    (context "/api/public/uraseuranta" [] :no-doc true :tags ["uraseuranta"] (arvo.rest-api.uraseuranta/uraseuranta-reitit asetukset))
    (context "/api/public/move" [] :no-doc true :tags ["move"] (arvo.rest-api.move/move-reitit asetukset))
    (context "/api/public/koodisto" [] :no-doc true :tags ["koodisto"] :middleware [#(wrap-authentication :kyselyynohjaus %)] arvo.rest-api.koodisto/reitit)
    (context "/api/tiedote" [] :no-doc true :tags ["tiedote"] :middleware [wrap-tarkasta-csrf-token] arvo.rest-api.tiedote/reitit)
    (context "/api/csv" [] :no-doc true :tags ["csv"] arvo.rest-api.raportti.kysely/csv)
    (context "/api/export/v1" [] :tags ["export"] :middleware [#(wrap-authentication :export %)] arvo.rest-api.export/v1)
    (context "/api/public/luovastaajatunnus" [] :no-doc true :tags ["vastaajatunnus"] :middleware [#(wrap-authentication :kyselyynohjaus %)] arvo.rest-api.automaattitunnus/kyselyynohjaus-v1)
    (context "/api/public/henkilo" [] :no-doc true :tags ["henkilooidit"] :middleware [#(wrap-authentication :kyselyynohjaus %)] arvo.rest-api.henkilo/hae-kaikki-oidit)
    (context "/api/vastauslinkki/v1" [] :tags ["vastauslinkki"] :middleware [#(wrap-authentication :ehoks_tunnukset %)] arvo.rest-api.automaattitunnus/ehoks-v1)
    (context "/api/tyoelamapalaute/v1" [] :tags ["tyoelamapalaute"] :middleware [#(wrap-authentication :ehoks_tunnukset %)] arvo.rest-api.automaattitunnus/tyoelamapalaute-v1)
    (context "/api/admin" [] :no-doc true :tags ["admin"] :middleware [#(wrap-authentication :admin %)] arvo.rest-api.admin/admin-routes)
    (r/not-found "Not found")))
