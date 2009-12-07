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

(defn fetch-current-stories-by [k]
  (apply merge-with concat
         (map (fn [[_ [i]]]
                (index-maps (-> i :iteration :stories)
                            #(-> % :story k)))
              (map-projects current))))

(defn serve-classpath-file
  "Serves a file off the classpath, i.e. bundled in the jar."
  ([path]
     (serve-classpath-file "public" path))
  ([root path]
     (ClassLoader/getSystemResource (str root path))))

(defroutes app
  (GET "/"
       (redirect-to (html/urls :current-by-project)))
  (GET (html/urls :current-by-project)
       (html/current-iterations (map-projects current)))
  (GET (html/urls :current-by-owner)
       (html/current-by "Owner" (fetch-current-stories-by :owned_by)))
  (GET (html/urls :current-by-requestor)
       (html/current-by "Requestor" (fetch-current-stories-by :requested_by)))
  (GET (html/urls :list-projects)
       (html/list-projects (*pt-user* projects)))
  (GET (html/urls :project-summary)
       (html/not-implemented "Project Summary"))
  (ANY "*"
       (or (serve-file (params :*))
           (serve-classpath-file (params :*))
           [404 (str "<h1>404 - Not Found:" (:uri request) "</h1>")])))

