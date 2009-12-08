(ns pivotal-panorama.html
  (:use [compojure :only [html unordered-list link-to redirect-to
                          include-css]]
        [clj-pt :only [user project projects current]])
  (:import [java.io File]))

(def urls {:current-by-project "/current/group/project"
           :current-by-owner   "/current/group/owned_by"
           :current-by-requestor   "/current/group/requestor"
           :list-projects "/projects"
           :project-summary "/projects/:id"})

(defn menu-items []
  (letfn [(menu-item [k title] [:li.menu-item (link-to (urls k) title)])
          (sep [] [:li.menu-item "|"])]
    [:ul#menu
     (menu-item :current-by-project "By Project")
     (sep)
     (menu-item :current-by-owner "By Owner")
     (sep)
     (menu-item :current-by-requestor "By Requestor" )]))

(defn html-document [title & body]
  (html
   [:html
    [:head [:title title]
     (include-css "/css/style.css")]
    [:body
     (menu-items)
     body]]))

(defn tag [& strings]
  (keyword (apply str strings)))

(defn html-card [m type card-class & keys]
  [(tag "div.card." type "-card" (when card-class
                                   (str "." card-class)))
   (map (fn [k]
          [(tag "div.card-content." type "-" (name k)) (k m)])
        keys)
   [:div.clear]])

(defn humanize [s]
  (.replaceAll s "_" " "))

(defn story-card [s]
  (let [story (merge s {:url (link-to (:url s) "Edit")
                        :current_state (:current_state s)})]
    (html-card story "story" (:current_state story)
               :name :description :owned_by :current_state :url)))

(defn grouped-story-page [title tuples]
  (html-document
   title
   (map (fn [[k vs]]
          (when (seq vs)
            [:div.project
             [:h1 k]
             (map story-card vs)
             [:div.clear]]))
        tuples)))

(defn current-by [title keys-and-stories]
  (grouped-story-page
   (str "Current by " title)
   (map (fn [k]
          (let [k (or k "Unassigned")]
            [k (keys-and-stories k)]))
        (sort (keys keys-and-stories)))))

(defn not-implemented [feature]
  (let [title (str "Not Implemented: " feature)]
    (html-document title [:h1 title])))

