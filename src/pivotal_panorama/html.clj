(ns pivotal-panorama.html
  (:use [compojure :only [html drop-down unordered-list link-to redirect-to
                          include-css include-js submit-button form-to]]
        [clj-pt :only [user project projects current]])

  (:import [java.io File]))

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

(def story-filter-options [["All" ""]
                           ["Unstarted" "unstarted"]
                           ["Started" "started"]
                           ["Finished" "finished"]
                           ["Delivered" "delivered"]
                           ["Accepted" "accepted"]
                           ["Rejected" "rejected"]])

(defn menu-items [iteration group-by story-filter]
  [:form
   [:div#menu
    [:span#iteration-item 
     [:span "Iteration:"]
     (drop-down "iteration" iteration-options iteration)]
    [:span#group-by-item
     [:span "Group By:"]
     (drop-down "group-by" group-by-options group-by)]
    [:span#story-filter
     [:span "Story:"]
     (drop-down "story-state" story-filter-options story-filter)]
    [:input {:type    "submit"
             :value   "Refresh"}]]])

(defn html-document [title & body]
  (html
   [:html
    [:head [:title title]
     (include-css "/css/style.css")
     (include-js "/js/jquery-1.3.2.min.js"
                 "/js/application.js")] 
    [:body
     body]]))

(defn tag [& strings]
  (keyword (apply str strings)))

(defn html-card [m type card-class title-key body-key & other-keys]
  [(tag "div.card." type "-card" (when card-class
                                   (str "." card-class)))
   [(tag "div.card-title." type "-" (name title-key)) (title-key m)]
   [(tag "div.card-body." type "-" (name body-key)) (body-key m)]
   [:div.clear]
   [:div.card-others
    (map (fn [k]
           (if (k m)
             [(tag "div.card-other." type "-" (name k)) (k m)]))
         other-keys)]])

(defn humanize [s]
  (.replaceAll s "_" " "))

(defn story-card [s]
  (let [story (merge s {:url (link-to (:url s) "edit")})
        card-class (str (:story_type story) "." (:current_state story))]
    (html-card story "story" card-class
               :name
               :description
               :accepted_at
               :current_state :story_type :owned_by
               :url)))

(defn grouped-story-page [iteration group-by story-filter tuples]
  (html-document
   (str iteration " by " (humanize group-by))
   (menu-items iteration group-by story-filter)   
   (map (fn [[k vs]]
          (when (seq vs)
            [:div.project
             [:h1 k]
             (map story-card vs)
             [:div.clear]]))
        tuples)))

(defn current-by [iteration group-by story-filter keys-and-stories]
  (grouped-story-page
   iteration group-by story-filter
   (map (fn [k] 
          [(or k "Unassigned") (keys-and-stories k)])
        (sort (keys keys-and-stories)))))

(defn not-implemented [feature]
  (let [title (str "Not Implemented: " feature)]
    (html-document title [:h1 title])))

