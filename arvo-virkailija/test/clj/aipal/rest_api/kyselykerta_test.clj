(ns arvo.rest-api.kyselykerta-test
  (:require [arvo.rest-api.rest-util :refer [rest-kutsu body-json]]
            [arvo.sql.test-data-util :refer :all]
            [arvo.sql.test-util :refer :all])
  (:use clojure.test))

(use-fixtures :each tietokanta-fixture)
