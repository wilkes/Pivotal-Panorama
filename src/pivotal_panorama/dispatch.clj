(ns pivotal-panorama.dispatch
  (:use [compojure :only [defroutes GET ANY serve-file redirect-to]]
        [clj-pt :only [user project projects current]]
        [clojure.contrib.seq-utils :only [flatten]]
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

(defn group-iteration [group-by story-filter p iteration]
  (let [story-filter #(if story-filter
                        (= (:current_state %) story-filter)
                        true)
        add-project-info #(merge % {:project-link (str "http://www.pivotaltracker.com/projects/" (p :id))
                                    :project-name (:name p)})
        stories (map add-project-info (filter story-filter (:stories iteration)))]
    (if (= group-by :project)
      {(:name p) stories}
      (index-maps stories group-by))))

(defn fetch-stories [iteration-fn group-by story-filter]
  (let [group-stories (fn [[p iterations]]
                          (map #(group-iteration group-by story-filter p %) iterations))]
    (apply merge-with concat
           (flatten (map group-stories (map-projects iteration-fn))))))

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
                     (fetch-stories iterfn (keyword group-by) story-filter))))
  (ANY "*"
       (or (serve-file (params :*))
           (serve-classpath-file (params :*))
           [404 (str "<h1>404 - Not Found:" (:uri request) "</h1>")])))
