(ns user
  (:require [todo.db :as db]
            [todo.server :as server]))

(comment

  (def server (server/start-server))
  (server/stop-server server)

  (def db (db/create))

  (def token (db/generate-or-refresh-token db "youngnh@gmail.com"))
  (db/token-valid? db "youngnh@gmail.com" token)

  (db/create-schema! db)
  (db/teardown-schema! db)

  (db/add-todo db {"name" "go to the store"})
  (db/todo-exists? db 2)

  (db/list-todos db)

  (db/toggle-todo db 1)
  (db/remove-todo db 1)

  (db/get-burndown db)
  (db/get-progress db))
