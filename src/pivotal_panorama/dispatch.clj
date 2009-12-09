(ns pivotal-panorama.dispatch
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
  (letfn [(project-map [p i] {(:name p) (:stories i)})
          (make-maps [[p iterations]]
                     (map #(project-map p %) iterations))]
    (apply merge-with concat
           (flatten (map make-maps projects-and-iterations)))))

(defn index-maps [ms index-fn]
  (apply merge-with concat
         (map (fn [v] {(index-fn v) [v]}) ms)))

(defn fetch-stories-by [iteration-fn group-by]
  (let [make-maps (fn [[_ iterations]]
                    (map #(index-maps (:stories %) group-by)
                         iterations))
        story-maps (filter identity (flatten (map make-maps (map-projects iteration-fn))))]
    (apply merge-with concat story-maps)))

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
  (GET (urls :group-by)
       (let [iteration (-> request :route-params :iteration)
             iterfn (resolve-action iteration)
             group-by (-> request :route-params :group-by)]
         (current-by iteration
                     group-by
                     (if (= group-by "project")
                       (group-by-project-name (map-projects iterfn))
                       (fetch-stories-by iterfn (keyword group-by))))))
  (ANY "*"
       (or (serve-file (params :*))
           (serve-classpath-file (params :*))
           [404 (str "<h1>404 - Not Found:" (:uri request) "</h1>")]))
)