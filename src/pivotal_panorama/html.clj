(ns pivotal-panorama.html
  (:use [compojure :only [html unordered-list link-to redirect-to
                          include-css]]
        [clj-pt :only [user project projects current]])
  (:import [java.io File]))

(defn html-document [title & body]
  (html
   [:html
    [:head [:title title]
     (include-css "/css/style.css")]
    [:body body]]))

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

(defn list-projects [projects]
  (html-document "Projects"
   [:h1 "Projects"]
   (unordered-list
    (map #(link-to (str "/projects/" (-> % :project :id))
                   (-> % :project :name))
         projects))))

(defn grouped-story-page [title tuples]
  (html-document
   title
   (map (fn [[k vs]]
          (when (seq vs)
            [:div.project
             [:h1 k]
             (map #(story-card (:story %)) vs)
             [:div.clear]]))
        tuples)))

(defn current-iterations [projects-and-iterations]
  (grouped-story-page "Current"
   (map (fn [[p [i]]]
          [(-> p :project :name)
           (-> i :iteration :stories)])
        projects-and-iterations)))

(defn current-iterations-by-owner [owners-and-stories]
  (grouped-story-page
   "Current by Owner"
   (map (fn [k]
          (let [k (or k "Unassigned")]
            [k (owners-and-stories k)]))
        (sort (keys owners-and-stories)))))

(defn not-implemented [feature]
  (let [title (str "Not Implemented: " feature)]
    (html-document title [:h1 title])))

