(ns aipal.toimiala.kayttajaoikeudet-test
  (:require [clojure.test :refer :all]
            [aipal.infra.kayttaja :refer [*kayttaja*]]
            [aipal.toimiala.kayttajaoikeudet :refer :all]))

(deftest sisaltaa-jonkin-rooleista?-sisaltaa
  (is (sisaltaa-jonkin-rooleista? #{"foo" "bar" "baz"}
                                  [{:rooli "jee"} {:rooli "bar"} {:rooli "joo"}])))

(deftest sisaltaa-jonkin-rooleista?-ei-sisalla
  (is (not (sisaltaa-jonkin-rooleista? #{"foo" "bar" "baz"}
                                       [{:rooli "jee"} {:rooli "asdf"} {:rooli "joo"}]))))

(deftest kayttajalla-on-jokin-rooleista-koulutustoimijassa?-on-rooli
  (binding [*kayttaja* {:aktiivinen-rooli {:organisaatio "KT1" :rooli "bar"}}]
    (is (kayttajalla-on-jokin-rooleista-koulutustoimijassa? #{"foo" "bar"} "KT1"))))

(deftest kayttajalla-on-jokin-rooleista-koulutustoimijassa?-ei-ole-roolia
  (binding [*kayttaja* {:aktiivinen-rooli {:organisaatio "KT1" :rooli "baz"}}]
    (is (not (kayttajalla-on-jokin-rooleista-koulutustoimijassa? #{"foo" "bar"} "KT1")))))

(deftest kayttajalla-on-jokin-rooleista-koulutustoimijassa?-ei-ole-roolia-tassa-koulutustoimijassa
  (binding [*kayttaja* {:aktiivinen-rooli {:organisaatio "KT2" :rooli "bar"}}]
    (is (not (kayttajalla-on-jokin-rooleista-koulutustoimijassa? #{"foo" "bar"} "KT1")))))
