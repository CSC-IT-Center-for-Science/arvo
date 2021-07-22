(ns arvo.infra.kayttaja.middleware-sql-test
  (:require [clojure.test :refer :all]
            [clojure.tools.logging :as log]
            [korma.core :as sql]
            [korma.db :as db]
            [arvo.sql.test-util :refer [exec-raw-fixture]]
            [arvo.integraatio.sql.korma :as taulut]
            [arvo.infra.kayttaja.vaihto :refer [with-kayttaja]]
            [arvo.infra.kayttaja.vakiot :refer [jarjestelma-uid]]
            [arvo.infra.kayttaja.middleware :refer :all]))

;; Testataan, että Postgren arvo.kayttaja-muuttujaan päätyy aina käyttäjän oma
;; UID, eikä esim. tietokantayhteyden edellisen käyttäjän UID.
(deftest ^:integraatio postgrelle-paatyy-aina-kayttajan-oma-uid
  (let [kayttajat (for [i (range 50)]
                    (str "test-" i))]
    (exec-raw-fixture
     #(with-kayttaja jarjestelma-uid nil nil
        (db/transaction
          (doseq [k kayttajat]
            (sql/insert taulut/kayttaja
              (sql/values {:oid k
                           :uid k
                           :voimassa true}))))))
    (exec-raw-fixture
      #(try
         (let [saikeiden-kysely-kayttajat (for [_ (range 5)]
                                            (repeatedly 200 (partial rand-nth kayttajat)))
               saikeiden-sql-kayttajat (doall
                                         (map deref
                                              (doall
                                                (for [saikeen-kysely-kayttajat saikeiden-kysely-kayttajat]
                                                  (future
                                                    (doall
                                                      (for [k saikeen-kysely-kayttajat]
                                                        (do
                                                          ((wrap-kayttaja (fn [_]
                                                                            (:current_setting
                                                                              (first
                                                                                (sql/exec-raw "select current_setting('arvo.kayttaja');"
                                                                                              :results)))))
                                                            {:username k})))))))))]
           (is (= saikeiden-kysely-kayttajat saikeiden-sql-kayttajat)))
         (finally
           (db/transaction
             (sql/delete taulut/kayttaja
               (sql/where {:oid [like "test-%"]}))))))))
