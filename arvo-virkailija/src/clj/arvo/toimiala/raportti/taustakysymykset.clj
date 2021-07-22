;; Copyright (c) 2015 The Finnish National Board of Education - Opetushallitus
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

(ns arvo.toimiala.raportti.taustakysymykset
  (:require [clj-time.core :as time]
            [oph.common.util.util :refer [some-value-with]]))

(def ^:private uusi-aipal-kaytossa (time/date-time 2015 1 1))


(defn yhdista-taustakysymysten-vastaukset
  [vastaus]
  vastaus)

(defn yhdista-taustakysymysten-kysymykset
  [kysymys]
  kysymys)

(defn yhdista-valtakunnalliset-taustakysymysryhmat [kysymysryhmat]
      kysymysryhmat)

(defn ^:private poista-kysymys-kysymysryhmasta
  [kysymysryhma poistettava-kysymys]
  (update-in kysymysryhma [:kysymykset]
             (fn [kysymykset]
               (remove (fn [kysymys] (= (:kysymysid kysymys)
                                        poistettava-kysymys))
                       kysymykset))))

(defn ^:private poista-taustakysymys-raportista
  [valtakunnallinen-raportti poistettava-taustakysymys]
  valtakunnallinen-raportti)

