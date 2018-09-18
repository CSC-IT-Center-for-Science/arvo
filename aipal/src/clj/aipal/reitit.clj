(ns aipal.reitit
  (:require [clojure.pprint :refer [pprint]]

            [cheshire.core :as cheshire]
            [compojure.api.exception :as ex]
            [compojure.api.sweet :refer [api context swagger-routes GET]]
            [compojure.route :as r]
            [stencil.core :as s]

            [oph.common.infra.csrf-token :refer [aseta-csrf-token wrap-tarkasta-csrf-token]]
            [aipal.asetukset :refer [service-path build-id project-version]]
            [aipal.basic-auth :refer [wrap-basic-authentication]]
            aipal.rest-api.i18n
            aipal.rest-api.kieli
            aipal.rest-api.kysely
            aipal.rest-api.kyselykerta
            aipal.rest-api.kyselypohja
            aipal.rest-api.kysymysryhma
            aipal.rest-api.ohje
            aipal.rest-api.oppilaitos
            aipal.rest-api.toimipaikka
            aipal.rest-api.rahoitusmuoto
            aipal.rest-api.raportti.kysely
            aipal.rest-api.raportti.kyselykerta
            aipal.rest-api.raportti.valtakunnallinen
            aipal.rest_api.js-log
            aipal.rest-api.vastaajatunnus
            arvo.rest-api.avopvastaajatunnus
            arvo.rest-api.uraseuranta
            arvo.rest-api.koodisto
            aipal.rest-api.kayttaja
            aipal.rest-api.tutkinto
            aipal.rest-api.tutkintotyyppi
            aipal.rest-api.koulutustoimija
            aipal.rest-api.tiedote
            aipal.rest-api.vipunen
            arvo.rest-api.export
            [compojure.api.middleware :as mw]
            [arvo.auth :refer [wrap-authentication]]
            [aipal.infra.kayttaja :refer [*kayttaja*]]))

(defn reitit [asetukset]
  (api
    {:exceptions {:handlers {:schema.core/error ex/schema-error-handler}}}
    (swagger-routes
        {:ui "/api-docs"
         :spec "/swagger.json"
         :data {:info {:title "Arvo API"
                       :description "Arvon rajapinnat. Sisältää sekä integraatiorajapinnat muihin järjestelmiin, että Arvon sisäiseen käyttöön tarkoitetut rajapinnat."}
                :basePath (str (service-path (get-in asetukset [:server :base-url])))
                :tags [{:name "export" :description "Kyselytietojen siirtorajapinta"}]}})
    (GET "/" [] {:status 200
                 :headers {"Content-type" "text/html; charset=utf-8"
                           "Set-cookie" (aseta-csrf-token (-> asetukset :server :base-url service-path))}
                 :body (s/render-file "public/app/index.html"
                         (merge {:base-url (-> asetukset :server :base-url)
                                 :vastaus-base-url (-> asetukset :vastaus-base-url)
                                 :current-user (:nimi *kayttaja*)
                                 :build-id @build-id
                                 :project-version @project-version
                                 :development-mode (pr-str (:development-mode asetukset))
                                 :ominaisuus (cheshire/generate-string (:ominaisuus asetukset))}
                                (when-let [cas-url (-> asetukset :cas-auth-server :url)]
                                  {:logout-url (str cas-url "/logout")})))})
    (context "/api/jslog" [] :middleware [wrap-tarkasta-csrf-token] aipal.rest_api.js-log/reitit)
    (context "/api/i18n" [] aipal.rest-api.i18n/reitit)
    (context "/api/kieli" []  aipal.rest-api.kieli/reitit)
    (context "/api/kyselykerta" [] :tags ["kyselykerta"] :middleware [wrap-tarkasta-csrf-token] aipal.rest-api.kyselykerta/reitit)
    (context "/api/kyselypohja" [] :tags ["kyselypohja"] aipal.rest-api.kyselypohja/tiedosto-reitit)
    (context "/api/kyselypohja" [] :tags ["kyselypohja"] :middleware [wrap-tarkasta-csrf-token] aipal.rest-api.kyselypohja/reitit)
    (context "/api/ohje" [] :tags ["ohje"] :middleware [wrap-tarkasta-csrf-token] aipal.rest-api.ohje/reitit)
    (context "/api/oppilaitos" [] :tags ["oppilaitos"] :middleware [wrap-tarkasta-csrf-token] aipal.rest-api.oppilaitos/reitit)
    (context "/api/rahoitusmuoto" [] :tags ["rahoitusmuoto"] :middleware [wrap-tarkasta-csrf-token] aipal.rest-api.rahoitusmuoto/reitit)
    (context "/api/toimipaikka" [] :tags ["toimipaikka"] :middleware [wrap-tarkasta-csrf-token] aipal.rest-api.toimipaikka/reitit)
    (context "/api/raportti/kysely" [] :tags ["raportti"] (aipal.rest-api.raportti.kysely/csv-reitit asetukset))
    (context "/api/raportti/kysely" [] :tags ["raportti"] :middleware [wrap-tarkasta-csrf-token] (aipal.rest-api.raportti.kysely/reitit asetukset))
    (context "/api/raportti/kyselykerta" [] :tags ["raportti"] (aipal.rest-api.raportti.kyselykerta/csv-reitit asetukset))
    (context "/api/raportti/kyselykerta" [] :tags ["raportti"] :middleware [wrap-tarkasta-csrf-token] (aipal.rest-api.raportti.kyselykerta/reitit asetukset))
    ;(context "/api/raportti/valtakunnallinen" [] (aipal.rest-api.raportti.valtakunnallinen/csv-reitit asetukset))
    ;(context "/api/raportti/valtakunnallinen" [] :middleware [wrap-tarkasta-csrf-token] (aipal.rest-api.raportti.valtakunnallinen/reitit asetukset))
    (context "/api/kysely" [] :tags ["kysely"] :middleware [wrap-tarkasta-csrf-token] aipal.rest-api.kysely/reitit)
    (context "/api/kysymysryhma" [] :tags ["kysymysryhma"]:middleware [wrap-tarkasta-csrf-token] aipal.rest-api.kysymysryhma/reitit)
    (context "/api/vastaajatunnus" [] :tags ["vastaajatunnus"] :middleware [wrap-tarkasta-csrf-token] aipal.rest-api.vastaajatunnus/reitit)
    (context "/api/kayttaja" [] :tags ["kayttaja"] :middleware [wrap-tarkasta-csrf-token] aipal.rest-api.kayttaja/reitit)
    (context "/api/tutkinto" [] :tags ["tutkinto"] :middleware [wrap-tarkasta-csrf-token] aipal.rest-api.tutkinto/reitit)
    (context "/api/tutkintotyyppi" [] :tags ["tutkinto"]  :middleware [wrap-tarkasta-csrf-token] aipal.rest-api.tutkintotyyppi/reitit)
    (context "/api/koulutustoimija" [] :tags ["koulutustoimija"] :middleware [wrap-tarkasta-csrf-token] aipal.rest-api.koulutustoimija/reitit)
    (context "/api/public/luovastaajatunnus" [] :tags ["vastaajatunnus"] (arvo.rest-api.avopvastaajatunnus/reitit asetukset))
    (context "/api/public/uraseuranta" [] :tags ["uraseuranta"] (arvo.rest-api.uraseuranta/uraseuranta-reitit asetukset))
    (context "/api/public/koodisto" [] :tags ["koodisto"] (arvo.rest-api.koodisto/koodisto-reitit asetukset))
    (context "/api/tiedote" [] :tags ["tiedote"] :middleware [wrap-tarkasta-csrf-token] aipal.rest-api.tiedote/reitit)
    (context "/api/csv" [] :tags ["csv"] aipal.rest-api.raportti.kysely/csv)
    (context "/api/vipunen" [] :tags ["vipunen"] :middleware [#(wrap-basic-authentication % asetukset)] aipal.rest-api.vipunen/reitit)
    (context "/api/export/v1" [] :tags ["export"] :middleware [#(wrap-authentication %)] arvo.rest-api.export/v1)
    (r/not-found "Not found")))
