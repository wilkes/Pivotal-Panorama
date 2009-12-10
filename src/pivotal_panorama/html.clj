(ns pivotal-panorama.html
  (:use [compojure :only [html drop-down unordered-list link-to redirect-to
                          include-css submit-button form-to]]
        [clj-pt :only [user project projects current]])

  (:import [java.io File]))

(def urls {:group-by "/group/:iteration/:group-by"})

(defn group-by-url [iteration group-by]
  (str "/group/" iteration "/" group-by))

(def iteration-options [["Current" "current"]
                        ["Backlog" "backlog"]
                        ["Done"     "done"]])

(def group-by-options [["Project" "project"]
                       ["Story Type" "story_type"]
                       ["State" "current_state"]
                       ["Requestor" "requested_by"]
                       ["Owner" "owned_by"]])

(defn menu-items [iteration group-by]
  [:form
   [:div#menu
    [:span#iteration-item 
     [:span "Iteration:"]
     (drop-down "iteration" iteration-options iteration)]
    [:span#group-by-item
     [:span "Group By:"]
     (drop-down "group-by" group-by-options group-by)]
    [:input {:type    "submit"
             :value   "Refresh"}]]])

(defn story-state-filter []
  (form-to [:get] (drop-down "story-state"
                ["All" "Accepted" "Delivered" "Finished"
                 "Rejected" "Started" "Unstarted"]) (submit-button "Filter")))

(defn html-document [title & body]
  (html
   [:html
    [:head [:title title]
     (include-css "/css/style.css")
     (include-js "/js/jquery-1.3.2.min.js"
                 "/js/application.js")] 
    [:body
     (story-state-filter)
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

(defn grouped-story-page [iteration group-by tuples]
  (html-document
   (str iteration " by " (humanize group-by))
   (menu-items iteration group-by)   
   (map (fn [[k vs]]
          (when (seq vs)
            [:div.project
             [:h1 k]
             (map story-card vs)
             [:div.clear]]))
        tuples)))

(defn current-by [iteration group-by keys-and-stories]
  (grouped-story-page
   iteration group-by
   (map (fn [k] 
          [(or k "Unassigned") (keys-and-stories k)])
        (sort (keys keys-and-stories)))))

(defn not-implemented [feature]
  (let [title (str "Not Implemented: " feature)]
    (html-document title [:h1 title])))

