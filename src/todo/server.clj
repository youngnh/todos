(ns todo.server
  (:require [aleph.http :as http]
            [aleph.netty]
            [compojure.core :refer [ANY defroutes]]
            [environ.core :refer [env]]
            [liberator.core :refer [defresource]]
            [ring.middleware.cookies :refer [wrap-cookies]]
            [ring.middleware.json :refer [wrap-json-params]]
            [todo.db :as db]))

(def PORT (parse-long (env :server-port)))

;; TODO: check Authorization header instead
(defn- user-authorized? [user]
  (fn [ctx]
    (let [uid (get-in ctx [:request :cookies "uid" :value])]
      (= user uid))))

(defn- todo-exists? [task-id]
  (fn [ctx]
    (let [{:keys [db]} ctx]
      (db/todo-exists? db task-id))))

(defresource tasks [user]
  :service-available? {:db (db/create)}
  :allowed-methods [:get :post]
  :malformed? (fn [ctx]
                (let [{:keys [json-params request-method]} (get ctx :request)]
                  (when (= request-method :post)
                    (let [task-name (get json-params "name")]
                      [(nil? task-name) {:task-name task-name}]))))
  :authorized? (user-authorized? user)
  :available-media-types ["application/json"]
  :post! (fn [ctx]
           (let [{:keys [db task-name]} ctx]
             {:task-id (db/add-todo db {:name task-name})}))
  :handle-created (fn [ctx]
                    (let [{:keys [task-id]} ctx]
                      {:status "ok"
                       :id task-id}))
  :handle-ok (fn [ctx]
               (let [{:keys [db]} ctx]
                 (db/list-todos db))))

(defresource task [user task-id]
  :service-available? {:db (db/create)}
  :allowed-methods [:delete]
  :malformed? (fn [ctx]
                (let [parsed-task-id (parse-long task-id)]
                  [(nil? parsed-task-id) {:task-id parsed-task-id}]))
  :authorized? (user-authorized? user)
  :available-media-types ["application/json"]
  :exists? (todo-exists? task-id)
  :delete! (fn [ctx]
             (let [{:keys [db task-id]} ctx]
               (db/remove-todo db task-id)))
  :handle-no-content nil)

(defresource task-toggle [user task-id]
  :service-available? {:db (db/create)}
  :allowed-methods [:post]
  :malformed? (fn [ctx]
                (let [parsed-task-id (parse-long task-id)]
                  [(nil? parsed-task-id) {:task-id parsed-task-id}]))
  :exists? (todo-exists? task-id)
  :can-post-to-missing? false
  :authorized? (user-authorized? user)
  :available-media-types ["application/json"]
  :post! (fn [ctx]
           (let [{:keys [db]} ctx]
             (db/toggle-todo db task-id)))
  :handle-created (fn [ctx]
                    {:status "ok"
                     :id task-id}))

(defresource chart-progress [user]
  :service-available? {:db (db/create)}
  :allowed-methods [:get]
  :authorized? (user-authorized? user)
  :available-media-types ["application/json"]
  :handle-ok (fn [ctx]
               (let [{:keys [db]} ctx]
                 (db/get-progress db))))

(defresource chart-burndown [user]
  :service-available? {:db (db/create)}
  :allowed-methods [:get]
  :authorized? (user-authorized? user)
  :available-media-types ["application/json"]
  :handle-ok (fn [ctx]
               (let [{:keys [db]} ctx]
                 (db/get-burndown db))))

(defroutes app
  (ANY "/:user/tasks" [user] (tasks user))
  (ANY "/:user/task/:task-id" [user task-id] (task user task-id))
  (ANY "/:user/tasks/:task-id/toggle" [user task-id] (task-toggle user task-id))
  (ANY "/:user/charts/progress" [user] (chart-progress user))
  (ANY "/:user/charts/burndown" [user] (chart-burndown user)))

(def handler
  (-> app
      wrap-cookies
      wrap-json-params))

(defn start-server
  []
  (http/start-server handler {:port PORT}))

(defn stop-server
  [server]
  (.close server))

(defn -main []
  (let [server (start-server)]
    (println "Server started on port" (aleph.netty/port server))))
