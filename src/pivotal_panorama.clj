(ns pivotal-panorama
  (:gen-class)
  (:use [compojure :only [run-server servlet]]
        [pivotal-panorama.controllers :only [app set-api-token]]
        [clj-pt :only [user]]
        [clojure.contrib.except :only [throw-if-not]]))

(load-file (format "%s/.pprc" (System/getProperty "user.home")))
(defn -main [& [port]]
  (set-api-token api-token)
  (run-server {:port (Integer. (or port 8080))}
              "/*"
              (servlet app)))