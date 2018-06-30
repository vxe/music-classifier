
(ns music-classifier.data
  (:require [clj-http.client]
            [clojure.data.codec.base64 :as b64]
            [music-classifier.auth :refer :all])
  (:use [com.rpl.specter]
        [clojure.java.shell :only [sh]]))

(def pitch-classes [:c :c# :d :d# :e :f :f# :g :g# :a :a# :b])

(def pitch-keys {:c 0 :c# 1 :d 2 :d# 3 :e 4 :f 5 :f# 6 :g 7 :g# 8 :a 9 :a# 10 :b 11})

(defn io_hit-api-endpoint--web [endpoint]
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

(def track-id-name-map (atom {}))

(defn build-track-id-map []
  (for [track (io_get-all-library-track-names--web)]
    (cond (= nil (:id track)) (prn (str  "id " (:id track) " is missing" ))
          (= nil (:name track)) (prn (str "track name " (:name track) " is missing"))
          :else
          (swap! track-id-name-map assoc (keyword (:id track))  (:name track)))))

(def analyzed-tracks (agent {}))

(defn get-audio-features-by-track-id [id]
  (cheshire.core/parse-string
   (:out
    (sh "curl"
        "-s"
        "-H"
        @D_access-token--atm
        (str "https://api.spotify.com/v1/audio-features/" id))) true))

(defn analyze-library []
  (pmap (fn [id]
          (if (or (:error ((keyword id) @analyzed-tracks))
                  (= nil ((keyword id) @analyzed-tracks)))
            (do
              (prn id " is nil")
              (future (send analyzed-tracks assoc (keyword id) (get-audio-features-by-track-id id))))))
        (select [ALL :id] (io_get-all-library-track-names--web))))

(defn lookup-track-name-by-id [id]
  (let [name ((keyword id) @track-id-name-map)]
    (if (= nil name)
      (prn id " is mising")
      name)))

(defn lookup-track-id-by-name [name]
  (remove nil? (for [[k v] @track-id-name-map]
                 (if (= name v)
                   k))))

(defn lookup-track-id-by-regex [regex]
  (remove nil? (for [[k v] @track-id-name-map]
                 (let [track-match (re-matches regex v)]
                   (if track-match
                     k
                     (build-track-id-map))))))

(defn lookup-audio-features-by-track-name [name]
((first (lookup-track-id-by-name name)) @analyzed-tracks))

;; (defn lookup-audio-features-by-track-regex [title]
;;   (let [track (lookup-track-id-by-regex title)]
;;     (cond (list? ((first track) @analyzed-tracks)) (prn track))
;;     :else
;;         ((first track) @analyzed-tracks)
;;     ))


    (defn lookup-audio-features-by-track-regex [title]
      ((first (lookup-track-id-by-regex title)) @analyzed-tracks))

(defn lookup-track-by-valence [comparison valence]
  (remove nil? (for [[k v] @analyzed-tracks]
                 (if (comparison (first (select [:valence] v)) valence)
                   (:id v)
                   (prn k)
                   ))))

(defn lookup-track-by-loudness [comparison loudness]
  (remove nil? (for [[k v] @analyzed-tracks]
                 (if (comparison (first (select [:loudness] v)) loudness)
                   (:id v)))))

(defn lookup-track-by-key [comparison key]
  (remove nil? (for [[k v] @analyzed-tracks]
                 (if (comparison (first (select [:key] v)) key)
                   (:id v)))))

(defn lookup-track-by-duration [comparison duration]
  (remove nil? (for [[k v] @analyzed-tracks]
                 (if (comparison (first (select [:duration] v)) duration)
                   (:id v)))))

(defn lookup-track-by-instrumentalness [comparison instrumentalness]
  (remove nil? (for [[k v] @analyzed-tracks]
                 (if (comparison (first (select [:instrumentalness] v)) instrumentalness)
                   (:id v)))))

(defn lookup-track-by-mode [comparison mode]
  (remove nil? (for [[k v] @analyzed-tracks]
                 (if (comparison (first (select [:mode] v)) mode)
                   (:id v)))))

(defn lookup-track-by-energy [comparison energy]
  (remove nil? (for [[k v] @analyzed-tracks]
                 (if (comparison (first (select [:energy] v)) energy)
                   (:id v)))))

(defn lookup-track-by-speechiness [comparison speechiness]
  (remove nil? (for [[k v] @analyzed-tracks]
                 (if (comparison (first (select [:speechiness] v)) speechiness)
                   (:id v)))))

(defn lookup-track-by-time_signature [comparison time_signature]
  (remove nil? (for [[k v] @analyzed-tracks]
                 (if (comparison (first (select [:time_signature] v)) time_signature)
                   (:id v)))))

(defn lookup-track-by-liveness [comparison liveness]
  (remove nil? (for [[k v] @analyzed-tracks]
                 (if (comparison (first (select [:liveness] v)) liveness)
                   (:id v)))))

(defn lookup-track-by-danceability [comparison danceability]
  (remove nil? (for [[k v] @analyzed-tracks]
                 (if (comparison (first (select [:danceability] v)) danceability)
                   (:id v)))))

(defn lookup-track-by-tempo [comparison tempo]
  (remove nil? (for [[k v] @analyzed-tracks]
                 (if (comparison (first (select [:tempo] v)) tempo)
                   (:id v)))))

(defn lookup-track-by-acousticness [comparison acousticness]
  (remove nil? (for [[k v] @analyzed-tracks]
                (if (comparison (first (select [:acousticness] v)) acousticness)
                  (:id v)))))

(defn debug:print-nil-tracks []
  (clojure.pprint/pprint (select [ALL ALL #(= nil (:valence %))]  @analyzed-tracks)))


(defn debug:find-nil-tracks []
  (select [ALL ALL #(= nil (:valence %))]  @analyzed-tracks))
