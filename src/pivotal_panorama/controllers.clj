(ns pivotal-panorama.controllers
  (:use [compojure :only [defroutes GET ANY serve-file redirect-to]]
        [clj-pt :only [user project projects current]]
        [clojure.contrib.seq-utils :only [flatten]]
        [pivotal-panorama.html :only [urls current-by]]
        clojure.contrib.pprint)
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

(defn fetch-stories-by [iteration-fn grouping]
  (let [make-maps (fn [[_ iterations]]
                    (map #(index-maps (:stories %) grouping)
                         iterations))
        story-maps (filter identity (flatten (map make-maps (map-projects iteration-fn))))]
    (apply merge-with concat story-maps)))

(defn story-filter [])
 ([let filter (if (nil? (params :story-filter)))]) 
(defn serve-classpath-file
  "Serves a file off the classpath, i.e. bundled in the jar."
  ([path]
     (serve-classpath-file "public" path))
  ([root path]
     (ClassLoader/getSystemResource (str root path))))

(defn resolve-action [s]
  (ns-resolve 'clj-pt (symbol s)))

(defroutes app
  (GET "/"
       (redirect-to "/group/current/project"))
  (GET "/group/current/project"
       (current-by "Project"
                   (group-by-project-name (map-projects current))))
  (GET (urls :group-by)
       (let [iteration-fn (-> request :route-params :iteration resolve-action)
             grouping (-> request :route-params :grouping)]
         (current-by grouping
                     (fetch-stories-by iteration-fn (keyword grouping)))))
  (ANY "*"
       (or (serve-file (params :*))
           (serve-classpath-file (params :*))
           [404 (str "<h1>404 - Not Found:" (:uri request) "</h1>")]))
)