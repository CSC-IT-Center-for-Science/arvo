(ns arvo.rest-api.kysymysryhma-lisays-test
  (:require [clojure.test :refer :all]
            [korma.core :as sql]
            [arvo.arkisto.kysymysryhma :as arkisto]
            [arvo.infra.kayttaja]
            [arvo.rest-api.kysymysryhma :refer [lisaa-kysymysryhma!]]
            [arvo.sql.test-data-util :as test-data]))

(defn arkisto-stub-fixture [f]
  (with-redefs [arkisto/hae (fn [kysymysryhmaid] {})
                arkisto/poista-kysymys! (fn [kysymysid])
                arkisto/poista-kysymyksen-monivalintavaihtoehdot! (fn [kysymysid])
                arkisto/lisaa-kysymys! (fn [kysymys] {})
                arkisto/lisaa-monivalintavaihtoehto! (fn [vaihtoehto] {})
                arkisto/paivita! (fn [kysymysryhma] kysymysryhma)]
    (f)))

(use-fixtures :each arkisto-stub-fixture)
