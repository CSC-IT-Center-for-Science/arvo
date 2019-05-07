(ns arvo.toimiala.raportti.csv
  (:require [clojure-csv.core :refer [write-csv]]
            [oph.common.util.http-util :refer [parse-iso-date]]
            [oph.common.util.util :refer [map-by]]
            [clojure.core.match :refer [match]]
            [aipal.toimiala.raportti.util :refer [muuta-kaikki-stringeiksi]]
            [arvo.db.core :as db]
            [aipal.arkisto.kysely :refer [aseta-jatkokysymysten-jarjestys hae-kyselyn-kysymykset]]
            [clj-time.core :as time]
            [clj-time.format :as f]
            [aipal.asetukset :refer [asetukset]]
            [arvo.util :refer [in?]]
            [aipal.integraatio.koodistopalvelu :refer [hae-kunnat]]))

(def default-translations {:fi {:vastaajatunnus "Vastaajatunnus"
                                :vastausaika "Vastausaika"
                                :tunnus "Vastaajatunnus"
                                :luotuaika "Luontiaika"
                                :url "Vastauslinkki"
                                :kysymysryhma "Kysymysryhma"
                                :kysymys "Kysymys"
                                :vastaus "Vastaus"
                                :voimassa_alkupvm "Voimassa alkaen"
                                :voimassa_loppupvm "Voimassaolo päättyy"
                                :tutkintotunnus "Tutkinto" :vastausten_lkm "Vastauksia" :vastaajien_lkm "Vastaajien lkm"
                                :tutkinto_selite "Tutkinnon nimi"
                                :hankintakoulutuksen_toteuttaja_selite "Hankintakoulutuksen toteuttajan nimi"
                                :toimipaikka_selite "Toimipaikan nimi"}
                           :sv {:vastaajatunnus "Svarskod"
                                :vastausaika "Svarstid"
                                :tunnus "Svarskod"
                                :luotuaika "Skapat"
                                :url "Svararkod"
                                :voimassa_alkupvm "Första svarsdag"
                                :voimassa_loppupvm "Sista svarsdag"
                                :tutkintotunnus "Tutkinto" :vastausten_lkm "Respondents antal" :vastaajien_lkm "Svarsantal"
                                :tutkinto_selite "Namn på examen"
                                :hankintakoulutuksen_toteuttaja_selite "Namn på anordnaren av anskaffad utbildning"
                                :toimipaikka_selite "Namn på verksamhetsställe"}
                           :en {:vastaajatunnus "Answer identifier" :vastausaika "Response time"
                                :tunnus "Answer identifier"
                                :luotuaika "TimeCreated"
                                :url "Credential"
                                :voimassa_alkupvm "ValidityStartDate"
                                :voimassa_loppupvm "ValidityEndDate"
                                :tutkintotunnus "Tutkinto" :vastausten_lkm "RespondentCount" :vastaajien_lkm "ResponseCount"
                                :tutkinto_selite "Name of degree"
                                :hankintakoulutuksen_toteuttaja_selite "Name of provider (procured training)"
                                :toimipaikka_selite "Name of operational unit"}})

(def delimiter \;)

(defn create-csv [data]
  (write-csv (muuta-kaikki-stringeiksi data)
             :delimiter delimiter
             :end-of-line "\r\n"))

(defn select-values-or-nil [m keyseq]
  (let [defaults (zipmap keyseq (repeat nil))]
    (map #(get (merge defaults m) %) keyseq)))

(defn translate [lang prop translations]
  (or (get-in translations [lang prop])
      (get-in translations [:fi prop])))

(defn format-date [datetime]
  (when datetime
    (f/unparse (f/formatters :date) datetime)))

(defn get-template-parts [q]
  (filter second (select-keys q [:kysymysid])))

(defn create-row-template [questions]
    (mapcat get-template-parts questions))

(defn get-question-group-text [questions entry]
  (let [question (some #(if (= (get % (first entry)) (second entry))%) questions)]
    (match [(first entry) (:jarjestys question)]
           [:kysymysid 0] [(:kysymysryhma_nimi question)]
           :else [""])))

(defn replace-control-chars [text]
  (clojure.string/escape text {\newline " " \tab " " delimiter \,}))

(defn translate-field [field lang obj]
  (let [translated (get obj (keyword (str field "_" (name lang))))]
    (if (not-empty translated)
      (replace-control-chars translated)
      (when (not= "fi" lang)
        (translate-field field "fi" obj)))))

(defn get-choice-text [choices lang answer]
  (let [kysymysid (:kysymysid answer)
        jarjestys (:numerovalinta answer)
        choice (some #(if (and (= kysymysid (:kysymysid %))
                               (= jarjestys (:jarjestys %))) %)
                     choices)]
    (translate-field "teksti" lang choice)))

(defn numero-tai-eos [answer]
  (match [(some? (:numerovalinta answer)) (some? (:en_osaa_sanoa answer))]
         [true _] (:numerovalinta answer)
         [false true] "eos"
         [false false] ""))

(defn get-answer-text [choices type answers lang]
  (match [type]
         ["arvosana"] (:numerovalinta (first answers))
         ["arvosana4_ja_eos"] (numero-tai-eos (first answers))
         ["arvosana6_ja_eos"] (numero-tai-eos (first answers))
         ["arvosana6"] (:numerovalinta (first answers))
         ["arvosana7"] (:numerovalinta (first answers))
         ["asteikko5_1"] (:numerovalinta (first answers))
         ["nps"] (:numerovalinta (first answers))
         ["monivalinta"] (->> answers
                              (map #(get-choice-text choices lang %))
                              (clojure.string/join ", "))
         ["likert_asteikko"] (:numerovalinta (first answers))
         ["vapaateksti"] (when (:vapaateksti (first answers))
                           (replace-control-chars (:vapaateksti (first answers))))
         ["kylla_ei_valinta"] (:vaihtoehto (first answers))
         :else ""))

(defn get-answer [answers choices lang [key value]]
  (let [answers-for-question (filter #(if (= (get % key) value) %) answers)
        first-answer (first answers-for-question)]
    [(get-answer-text choices (:vastaustyyppi first-answer) answers-for-question lang)]))

(defn get-value [tutkintotunnus-old entry]
  (let [entry-missing (nil? entry)
        value-missing (and (= "tutkinto" (:kentta_id entry)) (nil? (:arvo entry)))]
    (if (or entry-missing value-missing)
      tutkintotunnus-old
      (:arvo entry))))

(defn hae-taustatiedot [taustatiedot tutkintotunnus]
  (if (:tutkinto taustatiedot)
    taustatiedot
    (assoc taustatiedot :tutkinto tutkintotunnus)))

(defn hae-monivalinnat [questions]
  (let [monivalinnat (filter #(= "monivalinta" (:vastaustyyppi %)) questions)
        kysymysidt (map :kysymysid monivalinnat)]
    (db/hae-monivalinnat {:kysymysidt kysymysidt})))

(defn muuta-taustakysymykset [kysymykset]
  (if (every? :taustakysymys kysymykset)
    (map #(assoc % :taustakysymys false) kysymykset)
    kysymykset))

(defn poista-valiotsikot [kysymykset]
  (filter #(not= (:vastaustyyppi %) "valiotsikko") kysymykset))

(defn hae-kysymykset [kyselyid]
  (->> (hae-kyselyn-kysymykset kyselyid)
       flatten
       poista-valiotsikot
       muuta-taustakysymykset))

(defn csv-response [kyselyid lang data]
  (let [kysely (db/hae-kysely {:kyselyid kyselyid})
        koulutustoimija (db/hae-koulutustoimija {:ytunnus (:koulutustoimija kysely)})]
    {:nimi (translate-field "nimi" lang kysely)
     :koulutustoimija (translate-field "nimi" lang koulutustoimija)
     :date (f/unparse (f/formatters :date) (time/now))
     :csv data}))

(def default-csv-fields [:vastaajatunnus :vastausaika])
(def default-vastaajatunnus-fields [:tunnus :url :luotuaika :voimassa_alkupvm :voimassa_loppupvm :vastausten_lkm :vastaajien_lkm])

(defn get-csv-field [kentta]
  (if (get-in kentta [:raportointi :csv :selitteet])
    [(keyword (:kentta_id kentta)) (keyword (str (:kentta_id kentta) "_selite"))]
    (keyword (:kentta_id kentta))))

(defn taustatieto-kentat [taustatiedot]
  (->> taustatiedot
       (filter (comp :raportoi :csv :raportointi))
       (sort-by (comp :jarjestys :csv :raportointi))
       (map get-csv-field)
       flatten))

(defn get-csv-fields [taustatiedot]
  (concat default-csv-fields (taustatieto-kentat taustatiedot)))

(defn luo-käännökset [taustatiedot lang]
  (into (lang default-translations)
    (for [taustatieto (sort-by (comp :jarjestys :csv :raportointi) taustatiedot)]
      (let [translate-key (keyword (:kentta_id taustatieto))
            value (translate-field "kentta" lang taustatieto)]
        {translate-key value}))))

(defn hae-vastaus [kysymys vastaukset monivalintavaihtoehdot lang]
  (let [kysymyksen-vastaukset (filter  #(= (:kysymysid kysymys) (:kysymysid %)) vastaukset)]
    (get-answer-text monivalintavaihtoehdot (:vastaustyyppi kysymys) kysymyksen-vastaukset lang)))

(defn lisaa-selitteet [data selitteet lang]
  (-> data
      (assoc :tutkinto_selite
             (translate-field "nimi" lang
               (first (filter #(= (:tutkinto data) (:tutkintotunnus %)) (:tutkinnot selitteet)))))
      (assoc :toimipaikka_selite
             (translate-field "nimi" lang
               (first (filter #(= (:toimipaikka data) (:toimipaikkakoodi %)) (:toimipaikat selitteet)))))
      (assoc :hankintakoulutuksen_toteuttaja_selite
             (translate-field "nimi" lang
               (first (filter #(= (:hankintakoulutuksen_toteuttaja data) (:ytunnus %)) (:koulutustoimijat selitteet)))))
      (assoc :koulutusala_selite
             (translate-field "nimi" lang
               (first (filter #(= (:koulutusala data) (:koulutusalakoodi %)) (:koulutusalat selitteet)))))
      (assoc :kunta_selite
             (translate-field "nimi" lang
               (first (filter #(= (:kunta data) (:kuntakoodi %)) (:kunnat selitteet)))))))


(defn format-vastaus [vastaus selitteet lang]
  (-> (merge (:taustatiedot vastaus) vastaus)
      (update :vastausaika format-date)
      (lisaa-selitteet selitteet lang)))

(defn luo-vastausrivi [template lang taustatieto-fields choices selitteet answers]
  (let [formatted-answers (map #(format-vastaus % selitteet lang) answers)
        taustatiedot (select-values-or-nil (first formatted-answers) taustatieto-fields)]
    (concat taustatiedot (mapcat #(get-answer formatted-answers choices lang %) template))))

(defn luo-vastaajan-vastausrivit [[_ answers] kysymykset taustatieto-fields choices selitteet lang]
  (let [formatted-answers (map #(format-vastaus % selitteet lang) answers)
        taustatiedot (select-values-or-nil (first formatted-answers) taustatieto-fields)
        taustakysymysten-vastaukset (->> kysymykset
                                         (filter :taustakysymys)
                                         (map #(hae-vastaus % answers choices lang)))]
    (->> kysymykset
         (filter (complement :taustakysymys))
         (map #(concat taustatiedot taustakysymysten-vastaukset
                       [(translate-field "kysymysryhma" lang %) (translate-field "kysymys" lang %)
                        (hae-vastaus % answers choices lang)])))))

(defn create-header-row [header kysymykset lang translations]
  (let [header-fields (map #(get translations %) header)
        kysymys-fields (map #(translate-field "kysymys" lang %) kysymykset)]
    (concat header-fields kysymys-fields)))

(defn create-header-row-single [taustatieto-fields taustakysymykset translations]
  (concat (map #(get translations %) taustatieto-fields)
          taustakysymykset
          (map #(get translations %) [:kysymysryhma :kysymys :vastaus])))

(defn hae-selitteet [kyselyid]
  {:tutkinnot (db/hae-kyselyn-tutkinnot {:kyselyid kyselyid})
   :toimipaikat (db/hae-kyselyn-toimipaikat {:kyselyid kyselyid})
   :koulutustoimijat (db/hae-kyselyn-koulutustoimijat {:kyselyid kyselyid})
   :koulutusalat (db/hae-kyselyn-koulutusalat {:kyselyid kyselyid})
   :kunnat (hae-kunnat (:koodistopalvelu @asetukset))})

(defn luovutuslupa [[vastaajaid vastaukset] kysymysid]
  (= 0 (:numerovalinta (first (filter #(= kysymysid (:kysymysid %)) vastaukset)))))

(defn filter-not-allowed [kysymykset vastaukset]
  (let [lupakysymys (:kysymysid (first (filter #(= "tietojen_luovutus" (-> % :kategoria :taustakysymyksen_tyyppi)) kysymykset)))]
    (filter #(luovutuslupa % lupakysymys) vastaukset)))

(defn kysely-csv [kyselyid lang]
  (let [taustatiedot (db/kyselyn-kentat {:kyselyid kyselyid})
        taustatieto-fields (get-csv-fields taustatiedot)
        kysymykset (hae-kysymykset kyselyid)
        translations (luo-käännökset taustatiedot lang)
        vastaukset (filter-not-allowed kysymykset (group-by :vastaajaid (db/hae-vastaukset {:kyselyid kyselyid})))
        monivalintavaihtoehdot (hae-monivalinnat kysymykset)
        selitteet (hae-selitteet kyselyid)
        template (create-row-template kysymykset)
        header (create-header-row taustatieto-fields kysymykset lang translations)
        vastausrivit (map #(luo-vastausrivi template lang
                                            taustatieto-fields
                                            monivalintavaihtoehdot
                                            selitteet
                                            (second %)) vastaukset)]
    (csv-response kyselyid lang (create-csv (cons header vastausrivit)))))

(defn kysely-csv-vastauksittain [kyselyid lang]
  (let [taustatiedot (db/kyselyn-kentat {:kyselyid kyselyid})
        taustatieto-fields (get-csv-fields taustatiedot)
        kysymykset (hae-kysymykset kyselyid)
        selitteet (hae-selitteet kyselyid)
        translations (luo-käännökset taustatiedot lang)
        vastaukset (group-by :vastaajatunnus (db/hae-vastaukset {:kyselyid kyselyid}))
        monivalintavaihtoehdot (hae-monivalinnat kysymykset)
        taustakysymykset (->> kysymykset
                              (filter :taustakysymys)
                              (sort-by :jarjestys)
                              (map #(translate-field "kysymys" lang %)))
        header (create-header-row-single taustatieto-fields taustakysymykset translations)
        vastausrivit (mapcat #(luo-vastaajan-vastausrivit % kysymykset taustatieto-fields monivalintavaihtoehdot selitteet lang) vastaukset)]
    (csv-response kyselyid lang (create-csv (cons header vastausrivit)))))

(defn vastaajatunnus-url [tunnus]
  (str (:vastaus-base-url @asetukset) "/" (:tunnus tunnus)))

(defn create-header-row-single [taustatieto-fields taustakysymykset translations]
  (concat (map #(get translations %) taustatieto-fields)
          taustakysymykset
          (map #(get translations %) [:kysymysryhma :kysymys :vastaus])))

(defn format-tunnus [tunnus selitteet lang]
  (-> (merge (:taustatiedot tunnus) tunnus)
      (assoc :url (vastaajatunnus-url tunnus))
      (update :voimassa_alkupvm format-date)
      (update :voimassa_loppupvm format-date)
      (update :luotuaika format-date)
      (lisaa-selitteet selitteet lang)))

(defn vastaajatunnus-csv [kyselykertaid lang]
  (let [kyselyid (:kyselyid (db/hae-kyselykerta {:kyselykertaid kyselykertaid}))
        selitteet (hae-selitteet kyselyid)
        tunnukset (map #(format-tunnus % selitteet lang) (db/hae-vastaajatunnus {:kyselykertaid kyselykertaid}))
        taustatiedot (db/kyselyn-kentat {:kyselyid kyselyid})
        translations (luo-käännökset taustatiedot lang)
        vastaajatunnus-kentat (concat default-vastaajatunnus-fields (taustatieto-kentat taustatiedot))
        header (map #(get translations %) vastaajatunnus-kentat)
        rows (map #(select-values-or-nil % vastaajatunnus-kentat) tunnukset)]
    (create-csv (cons header rows))))
