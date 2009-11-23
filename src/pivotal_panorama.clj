(ns pivotal-panorama
  (:gen-class)
  (:use compojure))

(defroutes app
  (GET "/hello" "Hello, World!")
  (ANY "/*"      (or (serve-file (params :*)) :next))
  (ANY "*"       [404 (str "<h1>404 - Not Found:" (:uri request) "</h1>")]))

(defn -main [& args]
  (let [[port] args]
    (run-server {:port (Integer. (or port 8080))}
                "/*"
                (servlet app))))