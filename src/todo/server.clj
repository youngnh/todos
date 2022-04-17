(ns todo.server
  (:require [aleph.http :as http]
            [aleph.netty]
            [compojure.core :refer [ANY defroutes]]
            [environ.core :refer [env]]
            [liberator.core :refer [defresource]]
            [ring.middleware.cookies :refer [wrap-cookies]]
            [ring.middleware.params :refer [wrap-params]]))

(def PORT (parse-long (env :server-port)))

(defresource hello-world
  :available-media-types ["text/plain"]
  :handle-ok "Hello, world")

(defroutes app
  (ANY "/hello" [] hello-world))

(def handler
  (-> app
      wrap-cookies
      wrap-params))

(defn start-server
  []
  (http/start-server handler {:port PORT}))

(defn stop-server
  [server]
  (.close server))

(defn -main []
  (let [server (start-server)]
    (println "Server started on port" (aleph.netty/port server))))
