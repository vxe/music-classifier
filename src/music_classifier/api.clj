
(ns music-classifier.api
  (:require        
   [music-classifier.data :refer :all]
   [clojure.pprint]
   [cheshire.core]
   [clojure.tools.logging]
   [liberator.core :refer [resource defresource]]
   [ring.middleware.params :refer [wrap-params]]
   [compojure.core :refer [defroutes ANY]]))

(defroutes app
  (ANY "/callback" [] (resource :available-media-types ["text/html"]
                        :handle-ok (fn [ctx]
                                     (D-set-authorization-code! (get-in ctx [:request :params "code"])))))
  (ANY "/auth" [] (resource :available-media-types ["text/html"]
                        :handle-ok (fn [ctx]
                                     (D_set-access-token! (get-in ctx [:request :params "access_token"]))))))

(def handler 
  (-> app 
      wrap-params))
