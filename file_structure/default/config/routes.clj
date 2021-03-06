;; This file is used to route requests to the appropriate controller and action.

(ns routes
  (:require [conjure.util.loading-utils :as loading-utils]
            [clojure.contrib.logging :as logging]
            [clojure.contrib.str-utils :as contrib-str-utils]))

(defn draw []
  [ (fn [path]
      (if path
        (let [path-groups (re-matches #"/?(([^/]+)/?(([^/]+)/?([^/]+)?)?)?" path)]
          (if path-groups
            (let [controller (or (nth path-groups 2 nil) "home")
                  action (or (nth path-groups 4 nil) "index")
                  id (nth path-groups 5 nil)]
    
              (logging/debug "Using default router.")
              { :controller (loading-utils/underscores-to-dashes controller)
                :action (loading-utils/underscores-to-dashes action)
                :params (if id {:id id} {}) })))))])