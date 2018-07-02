(ns music-classifier.auth
  (:require        
   [clojure.pprint]
   [cheshire.core]
   [clojure.tools.logging]
   [liberator.core :refer [resource defresource]]
   [ring.middleware.params :refer [wrap-params]]
   [compojure.core :refer [defroutes ANY]]
   [clojure.data.codec.base64 :as b64])
  (:use [clojure.java.shell :only [sh]]))

(defn encode-base64 [original]
  (String. (b64/encode (.getBytes original)) "UTF-8"))

(def D_client-secret--atm (atom ""))

(def D_client-id--atm (atom ""))

(defn io_read-configuration--fs []
  (let [config-file (read-string (slurp "./.music-classifierrc"))
        client-secret  (:client-secret config-file)
        client-id (:client-id config-file)]
    (future (reset! D_client-secret--atm client-secret))
    (future (reset! D_client-id--atm client-id))))

(def D_authorization-code--atm (atom ""))
(defn D_set-authorization-code! [resp]
  (reset! D_authorization-code--atm resp))

(def D_refresh-token--atm (atom ""))

(def D_access-token--atm (atom ""))

(defn D_set-access-token! []
  (reset! D_refresh-token--atm
                     (-> "curl"
                         (sh
                          "-s"
                          "-H"
                          (str "Authorization: Basic " (encode-base64 (str
                                                                       @D_client-id--atm
                                                                       ":"
                                                                       @D_client-secret--atm)))
                          "-d"
                          "grant_type=authorization_code"
                          "-d"
                          (str "code=" @D_authorization-code--atm)
                          "-d"
                          (str "redirect_uri=" "http%3A%2F%2Flocalhost:8888%2Fcallback")
                          "https://accounts.spotify.com/api/token")
                         :out
                         (cheshire.core/parse-string true)
                         :refresh_token)))

(defn D_refresh-access-token! []
  (reset! D_access-token--atm
          (-> "curl"
              (sh
               "-s"
               "-H"
               (str "Authorization: Basic " (encode-base64 (str
                                                            @D_client-id--atm
                                                            ":"
                                                            @D_client-secret--atm)))
               "-d"
               "grant_type=refresh_token"
               "-d"
               (str "refresh_token=" @D_refresh-token--atm)
               "https://accounts.spotify.com/api/token")
              :out
              (cheshire.core/parse-string true)
              :access_token)))
