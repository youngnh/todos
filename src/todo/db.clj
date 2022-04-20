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
    (str token)))

(defn- now
  []
  (.. (java.time.Instant/now) (getEpochSecond)))

(defn token-valid?
  [todo-db user token]
  (let [{:keys [conn]} todo-db
        row (jdbc/execute-one! conn ["select token, expires from auth_token where user = ?" user])]
    (when row
      (and (= token (:auth_token/token row))
           (< (now) (:auth_token/expires row))))))

(defn list-todos
  [todo-db]
  (let [{:keys [conn user]} todo-db]
    (jdbc/execute! conn ["select * from todo_item where user = ?" user])))

(defn add-todo
  [todo-db todo]
  (let [{:keys [conn user]} todo-db
        {:keys [name]} todo]
    (-> (jdbc/execute-one! conn [(lines
                                  "insert into todo_item(user, name, created)"
                                  "values (?, ?, ?)")
                                 user
                                 name
                                 (now)]
                          {:return-keys true})
        (get (keyword "last_insert_rowid()")))))

(defn todo-exists?
  [todo-db task-id]
  (let [{:keys [conn user]} todo-db]
    (-> (jdbc/execute! conn ["select true from todo_item where id = ? and user = ?" task-id user])
        (count)
        (> 0))))

(defn toggle-todo
  [todo-db task-id]
  (let [{:keys [conn user]} todo-db]
    (jdbc/execute! conn [(lines
                          "update todo_item "
                          "  set completed=("
                          "    case when completed"
                          "      then null"
                          "      else ? end"
                          "  )"
                          "  where id = ? and user = ?")
                         (now)
                         task-id
                         user])))

(defn remove-todo
  [todo-db task-id]
  (let [{:keys [conn user]} todo-db]
    (jdbc/execute! conn ["delete from todo_item where id = ? and user = ?" task-id user])))

(defn get-progress
  [todo-db]
  (let [{:keys [conn user]} todo-db]
    (->> (jdbc/execute! conn [(lines
                               "select (case when completed is null then \"incomplete\" else \"complete\" end) as status,"
                               "  count() as count"
                               "from todo_item"
                               "where user = ?"
                               "group by (completed is null)")
                              user])
         (reduce (fn [result row]
                   (assoc result (keyword (:status row)) (:count row)))
                 {}))))

(defn get-burndown
  [todo-db]
  (let [{:keys [conn user]} todo-db]
    {:created (jdbc/execute! conn [(lines
                                    "select distinct created, count() over created_win as count"
                                    "  from todo_item"
                                    "  where user = ?"
                                    "  window created_win as (order by created)")
                                   user])
     :completed (jdbc/execute! conn [(lines
                                      "select distinct completed, count() over completed_win as count"
                                      "  from todo_item"
                                      "  where user = ? and completed is not null"
                                      "  window completed_win as (order by completed)")
                                     user])}))

(defn create-schema!
  [todo-db]
  (let [{:keys [conn]} todo-db]
    (jdbc/execute! conn [(lines
                          "create table if not exists todo_item ("
                          "  id integer primary key,"
                          "  user text,"
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

(defn clear-data!
  [todo-db]
  (let [{:keys [conn]} todo-db]
    (jdbc/execute! conn ["delete from todo_item"])
    (jdbc/execute! conn ["delete from auth_token"])))

(defrecord TodoDB [conn])

(defn create
  ([]
   (create default-spec nil))
  ([spec]
   (create spec nil))
  ([spec user]
   (map->TodoDB {:conn (jdbc/get-datasource spec)
                 :user user})))
