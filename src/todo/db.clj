(ns todo.db
  (:require [clojure.string :as string]
            [next.jdbc :as jdbc]))

(defn- lines
  [& strs]
  (string/join \newline strs))

(def default-spec {:dbtype "sqlite", :dbname "todo.db"})

(defn generate-or-refresh-token
  [todo-db user]
  (let [{:keys [conn]} todo-db
        token (random-uuid)]
    (jdbc/execute! conn [(lines
                          "insert into auth_token(user, token, expires)"
                          "values (?, ?, strftime('%s', 'now') + 3600)"
                          "on conflict (user)"
                          "do update set token = ?, expires = strftime('%s', 'now') + 3600")
                         user
                         token
                         token])
    token))

(defn list-todos
  [todo-db]
  (let [{:keys [conn]} todo-db]
    (jdbc/execute! conn ["select * from todo_item"])))

(defn add-todo
  [todo-db todo]
  (let [{:keys [conn]} todo-db
        {:keys [name]} todo]
    (->> (jdbc/execute! conn ["insert into todo_item(name, created) values (?, strftime('%s', 'now'))" name] {:return-keys true})
         (map #(get % (keyword "last_insert_rowid()")))
         (first))))

(defn todo-exists?
  [todo-db task-id]
  (let [{:keys [conn]} todo-db]
    (-> (jdbc/execute! conn ["select true from todo_item where id = ?" task-id])
        (count)
        (> 0))))

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

(defn get-progress
  [todo-db]
  (let [{:keys [conn]} todo-db]
    (->> (jdbc/execute! conn [(lines
                               "select (case when completed is null then \"incomplete\" else \"complete\" end) as status,"
                               "  count() as count"
                               "from todo_item"
                               "group by (completed is null)")])
         (reduce (fn [result row]
                   (assoc result (keyword (:status row)) (:count row)))
                 {}))))

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
                          "create table if not exists todo_item ("
                          "  id integer primary key autoincrement,"
                          "  name text,"
                          "  created integer,"
                          "  completed integer"
                          ");")])
    (jdbc/execute! conn [(lines
                          "create table if not exists auth_token ("
                          "  user text primary key,"
                          "  token text not null,"
                          "  expires integer not null"
                          ");")])))

(defn teardown-schema!
  [todo-db]
  (let [{:keys [conn]} todo-db]
    (jdbc/execute! conn ["drop table if exists todo_item"])
    (jdbc/execute! conn ["drop table if exists auth_token"])))

(defrecord TodoDB [conn])

(defn create
  ([]
   (create default-spec))
  ([spec]
   (map->TodoDB {:conn (jdbc/get-datasource spec)})))
