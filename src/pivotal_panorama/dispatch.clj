(ns pivotal-panorama.dispatch
  (:use [compojure :only [defroutes GET ANY serve-file redirect-to]]
        [clj-pt :only [user project projects current]]
        [clojure.contrib.seq-utils :only [flatten]]
        [clojure.contrib.str-utils2 :only [upper-case blank?]]
        [pivotal-panorama.html :only [current-by]]
        clojure.contrib.pprint)
  (:import [java.io File]))

(declare *pt-user*)
(defn set-api-token [token]
  (def *pt-user* (user token)))


(defn on-project [p args]
  (future [p (apply (*pt-user* project (-> p :id)) args)]))

(defn map-projects [& args]
  (map deref (map #(on-project % args) (*pt-user* projects))))

(defn index-maps [ms index-fn]
  (apply merge-with concat
         (map (fn [v] {(index-fn v) [v]}) ms)))

(defn fetch-stories [iteration-fn group-by]
  (let [make-maps (fn [[p iterations]]
                    (map (fn [iteration]
                           (let [stories (map (fn [story]
                                                (merge story
                                                       {:project-link (get-project-link p)}
                                                       {:project-name (:name p)}))
                                              (:stories iteration))]
                            (if (= group-by :project)
                             {(:name p) stories}
                             (index-maps stories group-by))))
                         iterations)
                    )
        story-maps (map make-maps (map-projects iteration-fn))]
    (apply merge-with concat (filter identity (flatten story-maps)))))

(defn get-project-link [proj]
  (str "http://www.pivotaltracker.com/projects/" (proj :id)))

(defn filter-by-story [story-state rs]
  (apply merge (map #(hash-map (first %) (filter (fn [s]  (= (upper-case (s :current_state))
                                                        (upper-case story-state)))
                             (second %))) rs)))

(defn filter-results [rs story-filter] (if-not
                                     (blank? story-filter)
                                   (filter-by-story story-filter rs)
                                   rs)) 

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
       (redirect-to "/current?group-by=project"))
  (GET "/:iteration"
       (let [iteration (-> request :route-params :iteration)
             iterfn (resolve-action iteration)
             group-by (params :group-by)
             story-filter (params :story-state)]
         (current-by iteration
                     group-by
                     story-filter
                     (filter-results (fetch-stories iterfn (keyword group-by)) story-filter))))
  (ANY "*"
       (or (serve-file (params :*))
           (serve-classpath-file (params :*))
           [404 (str "<h1>404 - Not Found:" (:uri request) "</h1>")]))
)