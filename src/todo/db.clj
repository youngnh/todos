(ns todo.db
  (:require [next.jdbc :as jdbc]))

(def default-spec {:dbtype "sqlite", :dbname "todo.db"})

(defn list-todos
  [todo-db]
  (let [{:keys [conn]} todo-db]
    (jdbc/execute! conn ["select * from todo_item"])))

(defn add-todo
  [todo-db todo]
  (let [{:keys [conn]} todo-db
        {:keys [name]} todo]
    (->> (jdbc/execute! conn ["insert into todo_item(name) values (?)" name] {:return-keys true})
         (map #(get % (keyword "last_insert_rowid()"))))))

(defn create-schema!
  [todo-db]
  (let [{:keys [conn]} todo-db]
    (jdbc/execute! conn ["create table todo_item (id integer primary key autoincrement, name text)"])))

(defn teardown-schema!
  [todo-db]
  (let [{:keys [conn]} todo-db]
    (jdbc/execute! conn ["drop table todo_item"])))

(defrecord TodoDB [conn])

(defn create
  ([]
   (create default-spec))
  ([spec]
   (map->TodoDB {:conn (jdbc/get-datasource spec)})))
