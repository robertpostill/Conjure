(ns test.controller.test-base
  (:use clojure.contrib.test-is
        conjure.controller.base)
  (:require [conjure.model.database :as database]
            [conjure.util.session-utils :as session-utils]
            [destroyers.controller-destroyer :as controller-destroyer]
            [destroyers.view-destroyer :as view-destroyer]
            [generators.controller-generator :as controller-generator]))

(def controller-name "test")
(def action-name "show")

(defn setup-all [function]
  (controller-generator/generate-controller-file 
    { :controller controller-name, :actions [action-name], :silent true })
  (function)
  (controller-destroyer/destroy-all-dependencies 
    { :controller controller-name, :actions [action-name], :silent true }))
        
(use-fixtures :once setup-all)

(defn
#^{:doc "Returns a redirect response map based on the given url and status, for use when testing."}
  redirect-map 
  ([url] (redirect-map url 302))
  ([url status] 
    { :status status, 
      :headers { "Location" url, "Connection" "close" }, 
      :body (str "<html><body>You are being redirected to <a href=\"" url "\">" url "</a></body></html>") }))

(deftest test-redirect-to-full-url
  (let [url "http://example.com/home"]
    (is (= (redirect-map url) (redirect-to-full-url url)))
    (is (= (redirect-map url 301) (redirect-to-full-url url 301)))))

(deftest test-redirect-to
  (is (= (redirect-map "http://www.conjureapp.com") (redirect-to "http://www.conjureapp.com")))
  (is (= 
    (redirect-map "http://www.conjureapp.com/home/welcome") 
    (redirect-to { :scheme :http, :server-name "www.conjureapp.com" } "/home/welcome")))
  (let [request-map { :request { :scheme :http, :server-name "www.conjureapp.com" } :controller "home" :action "welcome" }]
    (is (= 
      (redirect-map "http://www.conjureapp.com/home/welcome") 
      (redirect-to request-map)))
    (is (= 
      (redirect-map "http://www.conjureapp.com/home/goodbye") 
      (redirect-to request-map { :controller "home", :action "goodbye" } )))
    (is (= 
      (redirect-map "http://www.conjureapp.com/home/goodbye" 301)
      (redirect-to request-map { :controller "home", :action "goodbye", :status 301 })))))