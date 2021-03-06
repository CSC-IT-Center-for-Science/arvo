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

(ns arvo.arkisto.organisaatiopalvelu
  (:require [korma.core :as sql]
            [arvo.integraatio.sql.korma :as taulut]
            [oph.korma.common :refer [select-unique-or-nil]]))

(defn hae-viimeisin-paivitys
  []
  (:paivitetty (select-unique-or-nil taulut/organisaatiopalvelu_log
                 (sql/order :id :desc)
                 (sql/limit 1)
                 (sql/fields :paivitetty))))

(defn ^:integration-api tallenna-paivitys!
  [ajankohta]
  (sql/insert taulut/organisaatiopalvelu_log
    (sql/values {:paivitetty ajankohta})))
