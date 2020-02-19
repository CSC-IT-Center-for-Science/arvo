(ns arvo.util
  (:require [arvo.config :refer [env]]))

(defn in? [coll elem]
  (some #(= elem %) coll))

(defn parse-int [number-string]
  (try (Integer/parseInt number-string)
    (catch Exception e nil)))

(defn format-url [base params]
  (str base "?"(->> params
                    (map #(str (name (first %))"=" (second %)))
                    (interpose "&")
                    (apply str))))

(defn service-path [base-url]
  (let [path (drop 3 (clojure.string/split base-url #"/"))]
    (str "/" (clojure.string/join "/" path))))

(defn api-response [body]
  {:status 200
   :body body
   :headers {"Content-Type" "application/json; charset=utf-8"}})

(defn paginated-response [data key page-length api-url params]
  (let [next-id (when (= page-length (count data)) (-> data last key))
        query-params (into {} (filter second params))
        next-url (format-url (str (-> env :server :base-url) api-url) (merge query-params {:since next-id}))]
    (if (some? data)
      (api-response {:data data
                     :pagination {:next_url (if next-id next-url nil)}})
      {:status 404})))
