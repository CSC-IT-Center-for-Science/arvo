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

(ns arvo.infra.eraajo
  "Säännöllisin väliajoin suoritettavat toiminnot."
  (:require [clojurewerkz.quartzite.scheduler :as qs]
            [clojurewerkz.quartzite.jobs :as j]
            [clojurewerkz.quartzite.triggers :as t]
            [clojurewerkz.quartzite.schedule.daily-interval :as s]
            [clojurewerkz.quartzite.schedule.cron :as cron]
            [clojure.tools.logging :as log]
            arvo.infra.eraajo.organisaatiot
            arvo.infra.eraajo.koulutustoimijoiden-tutkinnot
            arvo.infra.eraajo.raportointi
            arvo.infra.eraajo.koodistot
            arvo.infra.eraajo.automaattikyselyt)
  (:import arvo.infra.eraajo.organisaatiot.PaivitaOrganisaatiotJob
           arvo.infra.eraajo.koulutustoimijoiden_tutkinnot.PaivitaKoulutustoimijoidenTutkinnotJob
           arvo.infra.eraajo.raportointi.PaivitaNakymatJob
           arvo.infra.eraajo.koodistot.PaivitaKoodistotJob
           arvo.infra.eraajo.automaattikyselyt.LuoAutomaattikyselytJob))

(defn ajastus [asetukset tyyppi]
  (cron/schedule
    (cron/cron-schedule (get-in asetukset [:ajastus tyyppi]))))

(def ajastin (promise))

(defn mk-job [type identity job-data]
  (j/build
    (j/of-type type)
    (j/with-identity identity)
    (j/using-job-data job-data)))

(defn mk-trigger [identity schedule]
  (t/build
    (t/with-identity identity)
    (t/start-now)
    (t/with-schedule schedule)))

(defn ^:integration-api kaynnista-ajastimet! [asetukset]
  (log/info "Käynnistetään ajastetut eräajot")
  (when-not (realized? ajastin)
    (deliver ajastin (qs/initialize)))
  (log/info "Poistetaan vanhat jobit ennen uudelleenkäynnistystä")
  (qs/clear! @ajastin)
  (qs/start @ajastin)
  (log/info "Eräajomoottori käynnistetty")
  (let [org-job (mk-job PaivitaOrganisaatiotJob "paivita-organisaatiot" {"asetukset" (:organisaatiopalvelu asetukset)})
        org-trigger-daily (mk-trigger "daily3" (ajastus asetukset :organisaatiopalvelu))
        org-job2 (j/build
                   (j/of-type PaivitaOrganisaatiotJob)
                   (j/with-identity "paivita-organisaatiot2")
                   (j/using-job-data {"asetukset" (:organisaatiopalvelu asetukset)}))
        org-trigger-once (t/build
                          (t/with-identity "startup")
                          (t/start-now))
        koul-job (j/build
                   (j/of-type PaivitaKoulutustoimijoidenTutkinnotJob)
                   (j/with-identity "paivita-koulutustoimijoiden-tutkinnot"))
        koul-trigger-daily (mk-trigger "daily5" (ajastus asetukset :koulutustoimijoiden-tutkinnot))
        raportointi-job (j/build
                          (j/of-type PaivitaNakymatJob)
                          (j/with-identity "paivita-raportoinnin-nakymat"))
        raportointi-trigger (mk-trigger "raportointi" (ajastus asetukset :raportointi))
        koodistot-job (j/build
                        (j/of-type PaivitaKoodistotJob)
                        (j/with-identity "paivita-koodistot")
                        (j/using-job-data {"asetukset" (:koodistopalvelu asetukset)}))
        koodistot-trigger (mk-trigger "tutkinnot" (ajastus asetukset :tutkinnot))
        automaattikyselyt-job (j/build
                               (j/of-type LuoAutomaattikyselytJob)
                               (j/with-identity "luo-automaattikyselyt"))
        automaattikyselyt-trigger (mk-trigger "automaattikyselyt" (ajastus asetukset :automaattikyselyt))]
    (qs/schedule @ajastin org-job org-trigger-daily)
    (qs/schedule @ajastin org-job2 org-trigger-once)
    (qs/schedule @ajastin koul-job koul-trigger-daily)
    (qs/schedule @ajastin raportointi-job raportointi-trigger)
    (qs/schedule @ajastin koodistot-job koodistot-trigger)
    (qs/schedule @ajastin automaattikyselyt-job automaattikyselyt-trigger)))
