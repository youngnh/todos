(ns todo.db
  (:require [clojure.string :as string]
            [next.jdbc :as jdbc]))

(defn- lines
  [& strs]
  (string/join \newline strs))

(def default-spec {:dbtype "sqlite", :dbname "todo.db"})

(defn list-todos
  [todo-db]
  (let [{:keys [conn]} todo-db]
    (jdbc/execute! conn ["select * from todo_item"])))

(defn add-todo
  [todo-db todo]
  (let [{:keys [conn]} todo-db
        {:keys [name]} todo]
    (->> (jdbc/execute! conn ["insert into todo_item(name, created) values (?, strftime('%s', 'now'))" name] {:return-keys true})
         (map #(get % (keyword "last_insert_rowid()"))))))

(defn toggle-todo
  [todo-db task-id]
  (let [{:keys [conn]} todo-db]
    (jdbc/execute! conn [(lines
                          "update todo_item "
                          "  set completed=("
                          "    case when completed"
                          "      then null"
                          "      else strftime('%s', 'now') end"
                          "  )"
                          "  where id = ?")
                         task-id])))

(defn remove-todo
  [todo-db task-id]
  (let [{:keys [conn]} todo-db]
    (jdbc/execute! conn ["delete from todo_item where id = ?" task-id])))

(defn get-burndown
  [todo-db]
  (let [{:keys [conn]} todo-db]
    {:created (jdbc/execute! conn [(lines
                                    "select distinct created, count() over created_win as count"
                                    "  from todo_item"
                                    "  window created_win as (order by created)")])
     :completed (jdbc/execute! conn [(lines
                                      "select distinct completed, count() over completed_win as count"
                                      "  from todo_item"
                                      "  where completed is not null"
                                      "  window completed_win as (order by completed)")])}))

(defn create-schema!
  [todo-db]
  (let [{:keys [conn]} todo-db]
    (jdbc/execute! conn [(lines
                          "create table todo_item ("
                          "  id integer primary key autoincrement,"
                          "  name text,"
                          "  created integer,"
                          "  completed integer"
                          ");")])))

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
