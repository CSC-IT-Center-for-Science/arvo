(ns arvo.arkisto.kysely-sql-test
  (:require [clojure.test :refer :all]
            [arvo.arkisto.kysely :refer :all]
            [arvo.sql.test-data-util :as test-data]
            [arvo.sql.test-util :refer [tietokanta-fixture]]
            [arvo.arkisto.kyselykerta-sql-test :refer [lisaa-kyselykerta-ilman-vastaajia!]]))

(use-fixtures :each tietokanta-fixture)

(deftest ^:integraatio kysely-poistettavissa-test
  (testing "on poistettavissa jos vastaajia ei löydy"
    (testing "ja tila on luonnos"
      (let [kysely (test-data/lisaa-kysely! {:tila "luonnos"})]
        (is (kysely-poistettavissa? (:kyselyid kysely)))
        (poista-kysely! (:kyselyid kysely))))
    (testing "ja tila on suljettu"
      (let [kysely (test-data/lisaa-kysely! {:tila "suljettu"})]
        (is (kysely-poistettavissa? (:kyselyid kysely)))
        (poista-kysely! (:kyselyid kysely))))
    (testing "ja tila on suljettu, yksi kyselykerta"
      (let [kysely (test-data/lisaa-kysely! {:tila "suljettu"})
            kyselykerta (test-data/lisaa-kyselykerta! {} kysely)]
        (is (kysely-poistettavissa? (:kyselyid kysely)))
        (poista-kysely! (:kyselyid kysely)))))

  (testing "ei ole poistettavissa"
    (testing "jos tila on julkaistu"
      (let [kysely (test-data/lisaa-kysely! {:tila "julkaistu"})]
        (is (not (kysely-poistettavissa? (:kyselyid kysely))))))))

