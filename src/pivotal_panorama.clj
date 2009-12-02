(ns pivotal-panorama
  (:gen-class)
  (:use [compojure :only [run-server servlet]]
        [pivotal-panorama.controllers :only [app set-api-token]]
        [clj-pt :only [user]]
        [clojure.contrib.except :only [throw-if-not]]))

(defn -main [& [api-token port]]
  (throw-if-not api-token "A Pivotal Tracker API Token is required.")
  (set-api-token api-token)
  (run-server {:port (Integer. (or port 8080))}
              "/*"
              (servlet app)))