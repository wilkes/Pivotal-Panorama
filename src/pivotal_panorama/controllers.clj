(ns pivotal-panorama.controllers
  (:use [compojure :only [defroutes GET ANY serve-file
                          html unordered-list link-to]]
        [clj-pt :only [user project projects current]]))

(declare *pt-user*)
(defn set-api-token [token]
  (def *pt-user* (user token)))

(defn html-document [title & body]
  (html
   [:html
    [:head [:title title]]
    [:body
     (unordered-list (map #(apply link-to %) [["/projects" "Projects"]]))
     body]]))

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
  (map (fn [p] [p (apply (project-for (-> p :project :id)) args)])
       (*pt-user* projects)))

(defn tag [& strings]
  (keyword (apply str strings)))

(defn html-card [obj type card-class & keys]
  (letfn [(make-content [k] [(tag "div." type "-" (name k)) (k obj)])]
    [(tag "div." type "-card" (when card-class
                                (str "." card-class)))
     (map make-content keys)]))

(defn story-card [s]
  (let [story (merge s {:url (link-to (:url s) "View in PivotalTracker")
                        :current_state (.replaceAll (:current_state s) "_" " ")})]
    (html-card story "story" (:current_state story)
               :name :description :owned_by :current_state :url)))

(defn current-iterations []
  (html-document
   "Current"
   (map (fn [[project iteration]]
          [:div.project
           [:h3 (-> project :project :name)]
           (unordered-list
            (map #(story-card (:story %))
                 (-> iteration first :iteration :stories)))])
        (map-projects current))))

(defn not-implemented [feature-name]
  (let [title (str "Not Implemented: " feature-name)]
    (html-document
     title
     [:h1 title])))

(defroutes app
  (GET "/" (html-document "Pivotal Panorama" nil))
  (GET "/current" (current-iterations))
  (GET "/projects" (list-projects request))
  (GET "/projects/:id" (not-implemented "Project Summary"))
  (ANY "/*"(or (serve-file (params :*))
               :next))
  (ANY "*" [404 (str "<h1>404 - Not Found:" (:uri request) "</h1>")]))

