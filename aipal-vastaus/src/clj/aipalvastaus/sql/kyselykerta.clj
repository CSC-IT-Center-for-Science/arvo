;; Copyright (c) 2014 The Finnish National Board of Education - Opetushallitus
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

(ns aipalvastaus.sql.kyselykerta
  (:require [korma.core :as sql]
            [aipalvastaus.sql.korma :refer [vastaajatunnus-where]]
            [clojure.tools.logging :as log]))

(defn hae-kysymysryhmat [tunnus]
  (sql/select :kysymysryhma
    (sql/join :inner :kysely_kysymysryhma (= :kysymysryhma.kysymysryhmaid :kysely_kysymysryhma.kysymysryhmaid))
    (sql/join :inner :kyselykerta (= :kyselykerta.kyselyid :kysely_kysymysryhma.kyselyid))
    (sql/join :inner :vastaajatunnus (= :vastaajatunnus.kyselykertaid :kyselykerta.kyselykertaid))
    (sql/fields :kysymysryhma.kysymysryhmaid
                :kysymysryhma.nimi_fi
                :kysymysryhma.nimi_sv
                :kysymysryhma.nimi_en
                :kysymysryhma.kuvaus_fi
                :kysymysryhma.kuvaus_sv
                :kysymysryhma.kuvaus_en)
    (vastaajatunnus-where tunnus)
    (sql/order :kysely_kysymysryhma.jarjestys)))

(defn hae-kysymysryhmien-kysymykset [tunnus]
  (sql/select :kysymys
    (sql/join :inner :kysely_kysymys (= :kysely_kysymys.kysymysid :kysymys.kysymysid))
    (sql/join :inner :kyselykerta (= :kyselykerta.kyselyid :kysely_kysymys.kyselyid))
    (sql/join :inner :vastaajatunnus (= :vastaajatunnus.kyselykertaid :kyselykerta.kyselykertaid))
    (sql/join :left :kysymys_jatkokysymys (= :kysymys_jatkokysymys.jatkokysymysid :kysymys.kysymysid))
    (sql/fields :kysymys.kysymysryhmaid
                :kysymys.kysymysid
                :kysymys.vastaustyyppi
                :kysymys.monivalinta_max
                :kysymys.max_vastaus
                :kysymys.kysymys_fi
                :kysymys.kysymys_sv
                :kysymys.kysymys_en
                :kysymys.selite_fi
                :kysymys.selite_sv
                :kysymys.selite_en
                :kysymys.pakollinen
                :kysymys.eos_vastaus_sallittu
                :kysymys.jatkokysymys
                :kysymys.jarjestys
                :kysymys.rajoite
                [:kysymys_jatkokysymys.kysymysid :jatkokysymys_kysymysid]
                [:kysymys_jatkokysymys.vastaus :jatkokysymys_vastaus])
    (vastaajatunnus-where tunnus)
    (sql/order :kysymys.jarjestys)))

(defn hae-kysymysten-monivalintavaihtoehdot [tunnus]
  (sql/select :monivalintavaihtoehto
    (sql/join :inner :kysely_kysymys (= :kysely_kysymys.kysymysid :monivalintavaihtoehto.kysymysid))
    (sql/join :inner :kyselykerta (= :kyselykerta.kyselyid :kysely_kysymys.kyselyid))
    (sql/join :inner :vastaajatunnus (= :vastaajatunnus.kyselykertaid :kyselykerta.kyselykertaid))
    (sql/fields :monivalintavaihtoehto.monivalintavaihtoehtoid
                :monivalintavaihtoehto.jarjestys
                :monivalintavaihtoehto.kysymysid
                :monivalintavaihtoehto.teksti_fi
                :monivalintavaihtoehto.teksti_sv
                :monivalintavaihtoehto.teksti_en)
    (vastaajatunnus-where tunnus)
    (sql/order :monivalintavaihtoehto.jarjestys)))

(defn hae-kyselyn-tiedot [tunnus]
  (first
    (sql/select :kysely
      (sql/join :inner :kyselykerta (= :kyselykerta.kyselyid :kysely.kyselyid))
      (sql/join :inner :vastaajatunnus (= :vastaajatunnus.kyselykertaid :kyselykerta.kyselykertaid))
      (sql/join :left :tutkinto (= :tutkinto.tutkintotunnus :vastaajatunnus.tutkintotunnus))
      (sql/fields :kysely.nimi_fi
                  :kysely.nimi_sv
                  :kysely.nimi_en
                  :kysely.selite_fi
                  :kysely.selite_sv
                  :kysely.selite_en
                  :kysely.tyyppi
                  :uudelleenohjaus_url
                  :tutkinto.tutkintotunnus
                  [:tutkinto.nimi_fi :tutkinto_nimi_fi]
                  [:tutkinto.nimi_sv :tutkinto_nimi_sv]
                  [:tutkinto.nimi_en :tutkinto_nimi_en]
                  :kysely.sivutettu)
      (vastaajatunnus-where tunnus))))

(defn hae-vastaukset [tunnus]
  (sql/select :vastaus
    (sql/join :inner :vastaaja (= :vastaaja.vastaajaid :vastaus.vastaajaid))
    (sql/join :inner :vastaajatunnus (= :vastaajatunnus.vastaajatunnusid :vastaaja.vastaajatunnusid))
    (sql/join :inner :kysymys (= :kysymys.kysymysid :vastaus.kysymysid))
    (sql/fields :vastaus.vastausid
                :vastaus.kysymysid
                :kysymys.kysymysryhmaid
                :vastaus.vastaajaid
                :vastaus.numerovalinta
                :vastaus.vaihtoehto
                :vastaus.vapaateksti
                :vastaus.en_osaa_sanoa)
    (vastaajatunnus-where tunnus)))

(defn ^:private yhdista-monivalintavaihtoehdot-kysymyksiin [kysymykset monivalintavaihtoehdot]
  (let [kysymysid->monivalinnat (group-by :kysymysid monivalintavaihtoehdot)]
    (for [kysymys kysymykset]
      (assoc kysymys :monivalintavaihtoehdot (kysymysid->monivalinnat (:kysymysid kysymys))))))

(defn ^:private yhdista-tietorakenteet [kysymysryhmat kysymykset monivalintavaihtoehdot]
  (let [kysymysryhmaid->kysymykset (group-by :kysymysryhmaid (yhdista-monivalintavaihtoehdot-kysymyksiin kysymykset monivalintavaihtoehdot))]
    (for [kysymysryhma kysymysryhmat]
      (assoc kysymysryhma :kysymykset (kysymysryhmaid->kysymykset (kysymysryhma :kysymysryhmaid))))))

(defn jarjestysnumero-jatkokysymykselle [kysymykset kysymys]
  (if (:jatkokysymys kysymys)
    (let [jatkokysymys_kysymys (first (filter #(= (:kysymysid %) (:jatkokysymys_kysymysid kysymys)) kysymykset))]
      (assoc kysymys :jarjestys (+ (:jarjestys jatkokysymys_kysymys) 0.1)))
    kysymys))

(defn hae-kysymysryhmat-ja-kysymykset [tunnus]
  (let [kysymysryhmat (hae-kysymysryhmat tunnus)
        kysymykset (hae-kysymysryhmien-kysymykset tunnus)
        jarjestetty (map #(jarjestysnumero-jatkokysymykselle kysymykset %) kysymykset)
        monivalintavaihtoehdot (hae-kysymysten-monivalintavaihtoehdot tunnus)]
    (yhdista-tietorakenteet kysymysryhmat (sort-by :jarjestys jarjestetty) monivalintavaihtoehdot))

;(defn hae-kysymykset [tunnus]
  (let [kysymykset (hae-kysymysryhmien-kysymykset tunnus)
        monivalintavaihtoehdot (hae-kysymysten-monivalintavaihtoehdot tunnus)]
    (yhdista-monivalintavaihtoehdot-kysymyksiin kysymykset monivalintavaihtoehdot)))

(defn hae
  "Hakee kyselyn tiedot tunnuksella"
  [tunnus]
  (merge (hae-kyselyn-tiedot tunnus)
         {:kysymysryhmat (hae-kysymysryhmat-ja-kysymykset tunnus)}))
