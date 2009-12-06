(ns pivotal-panorama.controllers
  (:require [pivotal-panorama.html :as html])
  (:use [compojure :only [defroutes GET ANY serve-file redirect-to]]
        [clj-pt :only [user project projects current]]
        [clojure.contrib.seq-utils :only [flatten]])
  (:import [java.io File]))

(declare *pt-user*)
(defn set-api-token [token]
  (def *pt-user* (user token)))

(defn on-project [p args]
  (future [p (apply (*pt-user* project (-> p :project :id)) args)]))

(defn map-projects [& args]
  (map deref (map #(on-project % args) (*pt-user* projects))))

(defn index-maps [ms index-fn]
  (apply merge-with concat
   (map (fn [v] {(index-fn v) [v]}) ms)))

(defn fetch-current-by-owner []
  (apply merge-with concat
         (map (fn [[_ [i]]]
                (index-maps (-> i :iteration :stories)
                            #(-> % :story :owned_by)))
              (map-projects current))))

(defn serve-classpath-file
  "Serves a file off the classpath, i.e. bundled in the jar."
  ([path]
     (serve-classpath-file "public" path))
  ([root path]
     (ClassLoader/getSystemResource (str root path))))

(defroutes app
  (GET "/"
       (redirect-to "/current/group/project"))
  (GET "/current/group/project"
       (html/current-iterations (map-projects current)))
  (GET "/current/group/owned_by"
       (html/current-iterations-by-owner (fetch-current-by-owner)))
  (GET "/projects"
       (html/list-projects (*pt-user* projects)))
  (GET "/projects/:id"
       (html/not-implemented "Project Summary"))
  (ANY "*"
       (or (serve-file (params :*))
           (serve-classpath-file (params :*))
           [404 (str "<h1>404 - Not Found:" (:uri request) "</h1>")])))

