(ns arvo.arkisto.koulutustoimija-sql-test
  (:require
    [arvo.sql.test-util :refer :all]
    [arvo.sql.test-data-util :refer :all]
    [arvo.arkisto.koulutustoimija :as koulutustoimija-arkisto])
  (:use clojure.test))

(use-fixtures :each tietokanta-fixture)

(deftest ^:integraatio koulutustoimijan-luonti
  (testing "Haku palauttaa lisää-kutsulla luodun koulutustoimijan"
    (let [lkm-alussa (count (koulutustoimija-arkisto/hae-kaikki))
          lisatty (lisaa-koulutustoimija! {:ytunnus "7654321-9" :nimi_fi "burp"})
          kaikki (koulutustoimija-arkisto/hae-kaikki)
          lkm-lopussa (count kaikki)]
      (is (= (+ 1 lkm-alussa) lkm-lopussa))
      (is (some #(= (:ytunnus lisatty) (:ytunnus %)) kaikki)))))

