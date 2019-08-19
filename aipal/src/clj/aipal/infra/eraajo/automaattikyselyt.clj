(ns aipal.infra.eraajo.automaattikyselyt
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log]
            [arvo.db.core :refer [*db*] :as db])
  (:import (org.quartz Job)))

(defn luo-kysely! [koulutustoimija kuvaus tx]
  (let [kyselyid (:kyselyid (first (db/luo-kysely! tx (merge kuvaus {:tila "julkaistu" :koulutustoimija (:ytunnus koulutustoimija)
                                                                     :kayttaja "JARJESTELMA" :tyyppi (:kyselytyyppi kuvaus)
                                                                     :kategoria {:automatisointi_tunniste (:tunniste kuvaus)}}))))]
    (db/liita-kyselyn-kyselypohja! tx {:kyselyid kyselyid :kyselypohjaid (:kyselypohjaid kuvaus) :kayttaja "JARJESTELMA"})
    (db/luo-kyselykerta! tx {:kyselyid kyselyid :nimi (:kyselykerta_nimi kuvaus) :kayttaja "JARJESTELMA"
                             :automaattinen true :kategoria (:kyselykerta_kategoria kuvaus) :voimassa_alkupvm (:voimassa_alkupvm kuvaus)})
    kyselyid))

(defn luo-kyselyt! [kuvaus tx]
  (let [koulutustoimijat (db/hae-automaattikysely-koulutustoimijat tx kuvaus)
        _ (log/info "Luodaan automaattikyselyt" (count koulutustoimijat) "koulutustoimijalle")]
    (doall (for [k koulutustoimijat]
             (luo-kysely! k kuvaus tx)))))

(defn luo-automaattikyselyt []
  (let [kuvaukset (db/hae-automaattikysely-data)
        kyselyidt (jdbc/with-db-transaction [tx *db*]
                    (flatten (doall (for [kuvaus kuvaukset]
                                      (luo-kyselyt! kuvaus tx)))))]
    (log/info "Luotu" (count kyselyidt) "automaattikyselyä: " kyselyidt)))

(defrecord LuoAutomaattikyselytJob []
  Job
  (execute [this ctx]
    (try
      (luo-automaattikyselyt)
      (catch Exception e
        (log/error "Automaattikyselyiden luonti epäonnistui"
            (map str (.getStackTrace e)))))))