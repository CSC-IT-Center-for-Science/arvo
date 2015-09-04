(ns aipal.rest-api.kysymysryhma-muokkaus-test
  (:require [clojure.test :refer :all]
            [korma.core :as sql]
            [aipal.arkisto.kysymysryhma :as arkisto]
            [aipal.rest-api.kysymysryhma :refer [paivita-kysymysryhma!]]
            [aipal.sql.test-data-util :as test-data]))

(defn arkisto-stub-fixture [f]
  (with-redefs [arkisto/hae (fn [kysymysryhmaid] {})
                arkisto/poista-kysymys! (fn [kysymysid])
                arkisto/poista-kysymyksen-monivalintavaihtoehdot! (fn [kysymysid])
                arkisto/lisaa-jatkokysymys! (fn [jatkokysymys] {})
                arkisto/lisaa-kysymys! (fn [kysymys] {})
                arkisto/lisaa-monivalintavaihtoehto! (fn [vaihtoehto] {})
                arkisto/paivita! (fn [kysymysryhma] kysymysryhma)]
    (f)))

(use-fixtures :each arkisto-stub-fixture)

(deftest paivita-kysymysryhma-poistaa-kysymyksen
  (let [poistettu-kysymysid (atom nil)]
    (with-redefs [arkisto/hae (constantly {:kysymysryhmaid 1
                                                              :kysymykset [{:kysymysid 2}]})
                  arkisto/poista-kysymys! (partial reset! poistettu-kysymysid)]
      (paivita-kysymysryhma! {:kysymysryhmaid 1})
      (is (= @poistettu-kysymysid 2)))))

(deftest paivita-kysymysryhma-poistaa-monivalintakysymyksen-vaihtoehdot
  (let [poista-vaihtoehdot-kysymykselta (atom nil)]
    (with-redefs [arkisto/hae
                  (constantly {:kysymysryhmaid 1
                               :kysymykset [{:kysymysid 2
                                             :vastaustyyppi "monivalinta"}]})
                  arkisto/poista-kysymyksen-monivalintavaihtoehdot!
                  (partial reset! poista-vaihtoehdot-kysymykselta)]
      (paivita-kysymysryhma! {:kysymysryhmaid 1})
      (is (= @poista-vaihtoehdot-kysymykselta 2)))))

(deftest paivita-kysymysryhma-poistaa-jatkokysymyksen
  (let [poista-jatkokysymysid (atom nil)]
    (with-redefs [arkisto/hae (constantly {:kysymysryhmaid 1
                                           :kysymykset [{:kysymysid 2
                                                         :jatkokysymysid 3}]})
                  arkisto/poista-jatkokysymys! (partial reset! poista-jatkokysymysid)]
      (paivita-kysymysryhma! {:kysymysryhmaid 1})
      (is (= @poista-jatkokysymysid 3)))))

(deftest paivita-kysymysryhma-lisaa-kysymyksen
 (let [lisaa-kysymys!-kutsut (atom [])]
   (with-redefs [arkisto/hae (constantly {:kysymysryhmaid 1
                                          :kysymykset []})
                 arkisto/lisaa-kysymys! #(swap! lisaa-kysymys!-kutsut conj %&)]
     (paivita-kysymysryhma! {:kysymysryhmaid 1
                             :kysymykset [{:kysymys_fi "Uusi"}]})
     (is (= (count @lisaa-kysymys!-kutsut) 1))
     (let [[[lisatty-kysymys]] @lisaa-kysymys!-kutsut]
       (is (= (:kysymysryhmaid lisatty-kysymys) 1))
       (is (= (:kysymys_fi lisatty-kysymys) "Uusi"))))))

(deftest paivita-kysymysryhma-paivittaa-perustiedot
  (with-redefs [arkisto/hae (constantly {:kysymysryhmaid 1
                                         :nimi_fi "Vanha"})]
    (is (= (:nimi_fi (paivita-kysymysryhma! {:kysymysryhmaid 1
                                             :nimi_fi "Uusi"}))
           "Uusi"))))

(deftest paivita-ntm-kysymysryhma
  (testing "ntm-kysymysryhman päivitys"
    (let [kysymysryhma (merge test-data/default-kysymysryhma {:ntm_kysymykset true})
          paivitetty-kysymysryhma (atom nil)]
      (with-redefs [arkisto/paivita! (partial reset! paivitetty-kysymysryhma)]
        (testing "on sallittu pääkäyttäjälle"
          (with-redefs [aipal.infra.kayttaja/yllapitaja? (constantly true)]
            (paivita-kysymysryhma! kysymysryhma)
            (is (true? (:ntm_kysymykset @paivitetty-kysymysryhma)))))

        (testing "on sallittu ntm-vastuukäyttäjälle"
          (with-redefs [aipal.infra.kayttaja/yllapitaja? (constantly false)
                        aipal.infra.kayttaja/ntm-vastuukayttaja? (constantly true)]
            (paivita-kysymysryhma! kysymysryhma)
            (is (true? (:ntm_kysymykset @paivitetty-kysymysryhma)))))

        (testing "ei ole sallittu muille käyttäjille"
          (with-redefs [aipal.infra.kayttaja/yllapitaja? (constantly false)
                        aipal.infra.kayttaja/ntm-vastuukayttaja? (constantly false)]
            (paivita-kysymysryhma! kysymysryhma)
            (is (false? (:ntm_kysymykset @paivitetty-kysymysryhma)))))))))
