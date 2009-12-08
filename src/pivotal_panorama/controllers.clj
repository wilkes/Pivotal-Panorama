(ns pivotal-panorama.controllers
  (:require [pivotal-panorama.html :as html])
  (:use [compojure :only [defroutes GET ANY serve-file redirect-to]]
        [clj-pt :only [user project projects current]]
        [clojure.contrib.seq-utils :only [flatten]]
        [pivotal-panorama.html :only [urls current-by]])
  (:import [java.io File]))

(declare *pt-user*)
(defn set-api-token [token]
  (def *pt-user* (user token)))

(defn on-project [p args]
  (future [p (apply (*pt-user* project (-> p :id)) args)]))

(defn map-projects [& args]
  (map deref (map #(on-project % args) (*pt-user* projects))))

(defn group-by-project-name [projects-and-iterations]
  (letfn [(make-map [[p [i]]] {(:name p) (:stories i)})]
    (apply merge (map make-map projects-and-iterations))))

(defn index-maps [ms index-fn]
  (apply merge-with concat
   (map (fn [v] {(index-fn v) [v]}) ms)))

(defn fetch-current-stories-by [k]
  (letfn [(make-map [[_ [i]]] (index-maps (:stories i) k))]
    (apply merge-with concat (map make-map (map-projects current)))))

(defn serve-classpath-file
  "Serves a file off the classpath, i.e. bundled in the jar."
  ([path]
     (serve-classpath-file "public" path))
  ([root path]
     (ClassLoader/getSystemResource (str root path))))

(defroutes app
  (GET "/"
       (redirect-to (html/urls :current-by-project)))
  (GET (urls :current-by-project)
       (current-by "Project"
                   (group-by-project-name (map-projects current))))
  (GET (urls :current-by-owner)
       (current-by "Owner"
                   (fetch-current-stories-by :owned_by)))
  (GET (urls :current-by-requestor)
       (current-by "Requestor"
                   (fetch-current-stories-by :requested_by)))
  (ANY "*"
       (or (serve-file (params :*))
           (serve-classpath-file (params :*))
           [404 (str "<h1>404 - Not Found:" (:uri request) "</h1>")]))
)