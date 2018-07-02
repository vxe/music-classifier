(ns music-classifier.api
  (:require        
   [music-classifier.auth :refer :all]
   [music-classifier.data :refer :all]
   [clojure.pprint]
   [cheshire.core]
   [clojure.tools.logging]
   [liberator.core :refer [resource defresource]]
   [ring.middleware.params :refer [wrap-params]]
   [compojure.core :refer [defroutes ANY]]))

(defn login []
  (let
      [key (do (println "login here\n https://accounts.spotify.com/authorize/?client_id=e11274026afa4840b9b715e7cb0d8fbb&response_type=code&redirect_uri=http://localhost:8888/callback&scope=playlist-read-private%20user-library-read&state=34fFs29kd09 ") (flush) (read-line))]
    (do
      (io_read-configuration--fs)
      (D_set-access-token!)
      (D_refresh-access-token!))))

(defroutes app
  (ANY "/callback" [] (resource :available-media-types ["text/html"]
                                :handle-ok (fn [ctx]
                                             (cond (not (nil? (get-in ctx [:request :params "access_token"])))
                                                   (get-in ctx [:request :params "refresh_token"])
                                                   :else
                                                   (D_set-authorization-code! (get-in ctx [:request :params "code"]))))))
  (ANY "/" [] (resource :available-media-types ["text/html"]
                        :handle-ok (fn [ctx]
                                     "welcome-friend"))))

(def handler 
  (-> app 
      wrap-params))
