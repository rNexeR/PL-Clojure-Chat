(ns clojure-rest.rooms
  (:import com.mchange.v2.c3p0.ComboPooledDataSource)
  (:use ring.util.response)
  (:use clojure.java.io)
  (:require 
  	[clojure-rest.db_config :refer :all]
  	[clojure.java.jdbc :as sql]
  	[clojure-rest.rooms_users :refer :all]
  )
)

(defn get-all-rooms []
  (response
    (sql/with-connection (db-connection)
      (sql/with-query-results results
        ["select * from rooms"]
        (into [] results)
      )
    )
  )
)

(defn get-room [room_name]
  (sql/with-connection (db-connection)
    (sql/with-query-results results
      ["select * from rooms where name = ?" room_name]
      (cond
        (empty? results) {:status 404 :body "Room not found"}
        :else (response (first results))
      )
    )
  )
)

(defn create-new-room [email_user room]
	(println (get room "name"))
  (sql/with-connection (db-connection)
    (sql/insert-record :rooms room)
  )
  (let [room_id (get room "name")]
	  (let [room_user {:room_id room_id :user_id email_user :is_admin true}]
	  	(create-new-room-user room_user)
	  )
	)
  (get-room (get room "name"))
)

(defn update-room [room_name room]
  (sql/with-connection (db-connection)
    (let [u_room (assoc room "name" room_name)]
      (sql/update-values :rooms ["name=?" room_name] u_room)
    )
  )
  (get-room room_name)
)

(defn delete-room [room_name]
  (sql/with-connection (db-connection)
    (sql/delete-rows :rooms ["name=?" room_name])
  )
  {:status 204}
)

(defn get-my-rooms [usr]
  (response
    (sql/with-connection (db-connection)
      (sql/with-query-results results
        ["select r.name from rooms r inner join 
            rooms_users ru on r.name = ru.room_id inner join
              users u on ru.user_id = u.email where u.email = ? and ru.is_admin = true" usr]
        (into [] results)
      )
    )
  )
)

(defn get-rooms-where-iam [usr]
  (response
    (sql/with-connection (db-connection)
      (sql/with-query-results results
        ["select rim.name, u.email as admin_email, u.realname as admin_name from (select r.name 
                    from rooms r inner join rooms_users ru on r.name = ru.room_id 
                      inner join users u on ru.user_id = u.email 
                        where ru.user_id = ? and ru.is_admin = false) rim
            inner join rooms_users ru on ru.room_id = rim.name
              inner join users u on ru.user_id = u.email where ru.is_admin = true" usr]
        (into [] results)
      )
    )
  )
)