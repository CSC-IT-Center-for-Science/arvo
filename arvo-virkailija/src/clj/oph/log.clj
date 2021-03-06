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

(ns oph.log
  (:require [clojure.tools.logging :as log]
            [robert.hooke :refer [add-hook]]
            [oph.common.infra.print-wrapper :as print-wrapper]
            [arvo.infra.kayttaja :refer [*kayttaja*]]))

(def ^:dynamic *lisaa-uid-ja-request-id?* true)

(defn lisaa-uid-ja-requestid
  [f logger level throwable message]
  (let [uid (if (bound? #'*kayttaja*)
              (:uid *kayttaja*)
              "-")
        requestid (if (bound? #'print-wrapper/*requestid*)
                    print-wrapper/*requestid*
                    "-")
        message-with-id (str "[User: " uid ", request: " requestid "] " message)]
    (if *lisaa-uid-ja-request-id?*
      (f logger level throwable message-with-id)
      (f logger level throwable message))))

(defn lisaa-uid-ja-requestid-hook []
  (add-hook #'log/log* #'lisaa-uid-ja-requestid))
