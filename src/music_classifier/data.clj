(ns music-classifier.data
  (:require [clj-http.client]
            [clojure.data.codec.base64 :as b64]
            ;; [clojure.contrib.sql :as sql]
            [music-classifier.auth :refer :all])
  (:use [com.rpl.specter]
        [clojure.java.shell :only [sh]]))

(def pitch-classes [:c :c# :d :d# :e :f :f# :g :g# :a :a# :b])

(def pitch-keys {:c 0 :c# 1 :d 2 :d# 3 :e 4 :f 5 :f# 6 :g 7 :g# 8 :a 9 :a# 10 :b 11})

(defn io_hit-tracks-endpoint--web [ids]
  (second
   (second
    (try 
      (cheshire.core/parse-string
       (:out
        (sh "curl"
            "-s"
            "-H"
            (str "Authorization: Bearer " @D_access-token--atm)
            (str "https://api.spotify.com/v1/me/tracks?limit=10"))) true)
          (catch Exception e
      (do (D_refresh-access-token!)
          (cheshire.core/parse-string
           (:out
            (sh "curl"
                "-s"
                "-H"
                @D_access-token--atm
                (str "https://api.spotify.com/v1/me/tracks?limit=10"))) true)))))))

(defn io_get-all-library-track-names--web []
  (flatten
   (distinct
    (select [ALL :track]
              (second
               (second
                (cheshire.core/parse-string
                 (:out
                  (sh "curl"
                      "-s"
                      "-H"
                      @D_access-token--atm
                      (str "https://api.spotify.com/v1/me/tracks?limit=10"))) true)))))))

(defn io_get-all-library-track-ids--web []
  (flatten
   (distinct
    (select [ALL :track :id]
              (second
               (second
                (cheshire.core/parse-string
                 (:out
                  (sh "curl"
                      "-s"
                      "-H"
                      @D_access-token--atm
                      (str "https://api.spotify.com/v1/me/tracks?limit=50"))) true))))
)))

(defn io_get-all-library-track-names--web []
  (flatten
   (distinct
    (select [ALL :track :name]
              (second
               (second
                (cheshire.core/parse-string
                 (:out
                  (sh "curl"
                      "-s"
                      "-H"
                      @D_access-token--atm
                      (str "https://api.spotify.com/v1/me/tracks?limit=50"))) true)))))))

(def atm--library (atom {}))

(defn io-web--build-track-id- [url]
  (let [current-offset
        (try
          (cheshire.core/parse-string (:body (clj-http.client/get url {:headers {"Authorization" (str "Bearer " @D_access-token--atm)}})) true)
          (catch Exception e
            (do
              (D_refresh-access-token!)
              (cheshire.core/parse-string (:body (clj-http.client/get url {:headers {"Authorization" (str "Bearer " @D_access-token--atm)}})) true))))]
    (if (nil? (:next current-offset))
      url
      (flatten (conj [url] (io-web--build-track-id- (:next current-offset)))))))

(defn io-web--get-all-track-ids []
  (flatten (for [offset-url
                 (io-web--build-track-id- "https://api.spotify.com/v1/me/tracks?offset=0&limit=50")]
             (for [track-data
                   (try
                     (:items (cheshire.core/parse-string (:body (clj-http.client/get offset-url  {:headers {"Authorization" (str "Bearer " @D_access-token--atm)}})) true))
                     (catch Exception e (do
                                          (D_refresh-access-token!)
                                          (:items (cheshire.core/parse-string (:body (clj-http.client/get offset-url  {:headers {"Authorization" (str "Bearer " @D_access-token--atm)}})) true)))))]
               (:id (:track track-data))))))

(for [id ["2qN4b7r3dpe8gLJfpKZGdk" "1MXPdYCJiVqTtMu32zFzvP"]] ;; should call get-all-track-ids
                        (swap! atm--library assoc (keyword id)
                               (into {}
                                     [{:track-info (try
                                        (cheshire.core/parse-string (:body (clj-http.client/get (str "https://api.spotify.com/v1/tracks/" id) {:headers {"Authorization" (str "Bearer " @D_access-token--atm)}})) true)
                                        (catch Exception e (do
                                                             (D_refresh-access-token!)
                                                             (cheshire.core/parse-string (:body (clj-http.client/get (str "https://api.spotify.com/v1/tracks/" id)  {:headers {"Authorization" (str "Bearer " @D_access-token--atm)}})) true))))} 
                                      {:audio-features (try
                                         (cheshire.core/parse-string (:body (clj-http.client/get (str "https://api.spotify.com/v1/audio-features/" id) {:headers {"Authorization" (str "Bearer " @D_access-token--atm)}})) true)
                                         (catch Exception e (do
                                                              (D_refresh-access-token!)
                                                              (cheshire.core/parse-string (:body (clj-http.client/get (str "https://api.spotify.com/v1/audio-features/" id)  {:headers {"Authorization" (str "Bearer " @D_access-token--atm)}})) true))))}]
                                                                    )
                          )

                        )
