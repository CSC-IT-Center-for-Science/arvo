(ns arvo.config
  (:require
    [cprop.core :refer [load-config]]
    [cprop.source :as source]
    [mount.core :refer [args defstate]]
    [clojure.java.io :as io])
  (:import (java.util Properties)))

(def project-version
  (delay (-> "project.clj" slurp read-string (nth 2))))

(defstate env
          :start
          (load-config
            :merge
            [(args)
             (source/from-system-props)
             (source/from-env)
             (try (source/from-props-file "aipal.properties") (catch java.io.FileNotFoundException e))]))
