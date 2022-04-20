(ns todo.db-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [next.jdbc :as jdbc]
            [todo.db :as db]))

(def test-spec {:dbtype "sqlite", :dbname "test.db"})

(def user-1 "alice@example.com")
(def user-2 "bob@example.com")

(def todos
  [{:name "load washing machine"}
   {:name "fold laundry"}
   {:name "take out the trash"}])

(defn create-schema
  [f]
  (let [db (db/create test-spec nil)]
    (db/teardown-schema! db)
    (db/create-schema! db))
  (f))

(defn clear-data
  [f]
  (let [db (db/create test-spec nil)]
    (db/clear-data! db))
  (f))

(use-fixtures :once create-schema)
(use-fixtures :each clear-data)

(deftest auth-token
  (let [db (db/create test-spec nil)
        token (db/generate-or-refresh-token db user-1)]
    (is (db/token-valid? db user-1 token))
    (let [refreshed-token (db/generate-or-refresh-token db user-1)]
      (is (not= token refreshed-token))
      (is (not (db/token-valid? db user-1 token)))
      (is (db/token-valid? db user-1 refreshed-token)))))

(deftest todo-crud
  (let [db-1 (db/create test-spec user-1)]
    (is (empty? (db/list-todos db-1)))
    (doseq [todo todos]
      (db/add-todo db-1 todo))

    (let [results (db/list-todos db-1)]
      (is (= (count todos) (count results)))
      (is (every? #(nil? (:todo_item/completed %)) results))
      (doseq [row results]
        (is (db/todo-exists? db-1 (:todo_item/id row)))
        (db/toggle-todo db-1 (:todo_item/id row))))

    (let [results (db/list-todos db-1)]
      (is (every? #(some? (:todo_item/completed %)) results))))

  (let [db-2 (db/create test-spec user-2)]
    (is (empty? (db/list-todos db-2)))
    (doseq [todo todos]
      (db/add-todo db-2 todo))
    (let [results (db/list-todos db-2)]
      (is (= (count todos) (count results)))
      (doseq [row results]
        (db/remove-todo db-2 (:todo_item/id row)))
      (is (empty (db/list-todos db-2))))))

(deftest charts
  (let [db-1 (db/create test-spec user-1)
        todos [{:name "task-1", :mock-creation 1, :mock-completion 10}
               {:name "task-2", :mock-creation 2, :mock-completion 11}
               {:name "task-3", :mock-creation 3, :mock-completion 11}
               {:name "task-4", :mock-creation 3, :mock-completion 12}]
        task-ids (for [todo todos]
                   (with-redefs [db/now (constantly (:mock-creation todo))]
                     (db/add-todo db-1 todo)))]

    (let [todo (first todos)
          task-id (first task-ids)]
      (with-redefs [db/now (constantly (:mock-completion todo))]
        (db/toggle-todo db-1 task-id)))

    (is (= {:complete 1
            :incomplete (dec (count todos))}
           (db/get-progress db-1)))

    (doseq [n (range 1 4)]
      (let [todo (nth todos n)
            task-id (nth task-ids n)]
        (with-redefs [db/now (constantly (:mock-completion todo))]
          (db/toggle-todo db-1 task-id))))

    (is (= {:created [{:todo_item/created 1
                       :count 1}
                      {:todo_item/created 2
                       :count 2}
                      {:todo_item/created 3
                       :count 4}]
            :completed [{:todo_item/completed 10
                         :count 1}
                        {:todo_item/completed 11
                         :count 3}
                        {:todo_item/completed 12
                         :count 4}]}
           (db/get-burndown db-1)))))
