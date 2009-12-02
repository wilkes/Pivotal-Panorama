(ns pivotal-panorama.controllers
  (:use [compojure :only [defroutes GET ANY serve-file
                          html unordered-list link-to]]
        [clj-pt :only [user projects]]))

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

(defn not-implemented [feature-name]
  (let [title (str "Not Implemented: " feature-name)]
    (html-document
     title
     [:h1 title])))

(defroutes app
  (GET "/" (html-document "Pivotal Panorama" nil))
  (GET "/projects" (list-projects request))
  (GET "/projects/:id" (not-implemented "Project Summary"))
  (ANY "/*"(or (serve-file (params :*))
               :next))
  (ANY "*" [404 (str "<h1>404 - Not Found:" (:uri request) "</h1>")]))

