(ns todo.db-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [todo.db :as db]))

(def test-spec {:dbtype "sqlite", :dbname "test.db"})
(def user-1 "alice@example.com")

(defn create-schema
  [f]
  (let [db (db/create test-spec)]
    (db/teardown-schema! db)
    (db/create-schema! db)
    (f)))

(use-fixtures :once create-schema)

(deftest auth-token
  (let [db (db/create test-spec)
        token (db/generate-or-refresh-token db user-1)]
    (is (db/token-valid? db user-1 token))
    (let [refreshed-token (db/generate-or-refresh-token db user-1)]
      (is (not= token refreshed-token))
      (is (not (db/token-valid? db user-1 token)))
      (is (db/token-valid? db user-1 refreshed-token)))))
