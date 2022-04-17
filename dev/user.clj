(ns user
  (:require [todo.db :as db]
            [todo.server :as server]))

(comment

  (def server (server/start-server))
  (def db (db/create))

  (db/create-schema! db)
  (db/teardown-schema! db)

  (db/add-todo db {:name "go to the store"})

  (db/list-todos db))
