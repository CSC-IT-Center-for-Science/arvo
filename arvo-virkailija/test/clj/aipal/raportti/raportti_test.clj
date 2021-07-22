(ns arvo.raportti.raportti-test
  (:require [clojure.test :refer :all]
    [arvo.sql.test-util :refer :all]
    [arvo.sql.test-data-util :refer :all]
    [arvo.toimiala.raportti.kysely :as kysely-raportti]
    [arvo.toimiala.raportti.valtakunnallinen :as valtakunnallinen-raportti]))

(use-fixtures :each tietokanta-fixture)

(def ^:private kysely1-arvosanatulos
  {:jakauma '({:vaihtoehto-avain "1", :lukumaara 0, :osuus 0} 
              {:vaihtoehto-avain "2", :lukumaara 1, :osuus 20}
              {:vaihtoehto-avain "3", :lukumaara 1, :osuus 20}
              {:vaihtoehto-avain "4", :lukumaara 0, :osuus 0}
              {:vaihtoehto-avain "5", :lukumaara 3, :osuus 60}),
   :eos_vastaus_sallittu nil, :keskiarvo 4.0000000000000000M, 
   :kysymys_en nil,
   :jarjestys 10, :vastaajien_lukumaara 5, 
   :kysymysid 7312026, :vastaustyyppi "arvosana", 
   :kysymys_sv "Hur bedömer du att ansökningsskedet lyckades som helhet?", 
   :kysymys_fi "Miten arvioit hakeutumisen onnistuneen kokonaisuutena?", 
   :keskihajonta 1.4142135623730950M})

(def valtakunnallinen-raportti-params
 {:taustakysymysryhmaid "3341885", :tyyppi "vertailu", :tutkintorakennetaso "tutkinto",
  :kysymykset {7312027 {:monivalinnat {}}, 
               7312028 {:monivalinnat {}}, 7312029 {:monivalinnat {}}, 
               7312030 {:monivalinnat {}}, 7312031 {:monivalinnat {}}, 
               7312032 {:monivalinnat {}}, 7312033 {:monivalinnat {}}}, 
  :vertailujakso_alkupvm nil, :vertailujakso_loppupvm nil})

