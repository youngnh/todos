(ns user
  (:require [todo.db :as db]
            [todo.server :as server]))

(comment

  (def server (server/start-server))
  (server/stop-server server)

  (def db (db/create))

  (db/create-schema! db)
  (db/teardown-schema! db)

  (db/add-todo db {"name" "go to the store"})

  (db/list-todos db)

  (db/toggle-todo db 1)
  (db/remove-todo db 1)

  (db/get-burndown db)
  (db/get-progress db))
