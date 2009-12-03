(ns pivotal-panorama.controllers
  (:use [compojure :only [defroutes GET ANY serve-file
                          html unordered-list link-to
                          include-css]]
        [clj-pt :only [user project projects current]]))

(declare *pt-user*)
(defn set-api-token [token]
  (def *pt-user* (user token)))

(defn html-document [title & body]
  (html
   [:html
    [:head [:title title]
     (include-css "/css/style.css")]
    [:body body]]))

(defn link-to-project [project]
  (link-to (str "/projects/" (-> project :project :id))
           (-> project :project :name)))

(defn list-projects [request]
  (html-document
   "Projects"
   [:h1 "Projects"]
   (unordered-list
    (map link-to-project (*pt-user* projects)))))

(defn project-for [id]
  (*pt-user* project id))

(defn map-projects [& args]
  (map deref (map (fn [p]
                    (future [p (apply (project-for (-> p :project :id)) args)]))
                  (*pt-user* projects))))

(defn tag [& strings]
  (keyword (apply str strings)))

(defn html-card [obj type card-class & keys]
  (letfn [(make-content [k] [(tag "div.card-content." type "-" (name k)) (k obj)])]
    [(tag "div.card." type "-card" (when card-class
                                (str "." card-class)))
     (map make-content keys)
     [:div.clear]]))

(defn story-card [s]
  (let [story (merge s {:url (link-to (:url s) "Edit")
                        :current_state (.replaceAll (:current_state s) "_" " ")})]
    (html-card story "story" (:current_state story)
               :name :description :owned_by :current_state :url)))

(defn current-iterations []
  (html-document
   "Current"
   (map (fn [[project iteration]]
          (when (seq (-> iteration first :iteration :stories))
            [:div.project
             [:h1 (-> project :project :name)]
             (map #(story-card (:story %))
                  (-> iteration first :iteration :stories))
             [:div.clear]]))
        (map-projects current))))

(defn group-maps-by-values [ms k]
  (apply merge-with concat
         (map (fn [v] {(k v) [v]}) ms)))

(defn current-iterations-by-owner []
  (letfn [(current-by-owner [[_ iteration]]
           (let [stories (-> iteration first :iteration :stories)]
             (group-maps-by-values stories #(-> % :story :owned_by))))]
    (html-document
     "Current by Owner"
     (map (fn [[owner stories]]
            (when (seq stories)
              [:div.project
               [:h1 owner]
               (map #(story-card (:story %)) stories)
               [:div.clear]]))
          (apply merge-with concat (map current-by-owner (map-projects current)))))))

(defn not-implemented [feature-name]
  (let [title (str "Not Implemented: " feature-name)]
    (html-document
     title
     [:h1 title])))

(defroutes app
  (GET "/" (current-iterations))
  (GET "/current-by-owner" (current-iterations-by-owner))
  (GET "/projects" (list-projects request))
  (GET "/projects/:id" (not-implemented "Project Summary"))
  (ANY "*"(or (serve-file (params :*))
              [404 (str "<h1>404 - Not Found:" (:uri request) "</h1>")])))
