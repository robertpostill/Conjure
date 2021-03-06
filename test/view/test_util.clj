(ns test.view.test-util
  (:import [java.io File])
  (:use test-helper
        clojure.contrib.test-is
        conjure.view.util)
  (:require [generators.view-generator :as view-generator]
            [destroyers.view-destroyer :as view-destroyer]))

(def action-name "show")
(def controller-name "test")

(defn setup-all [function]
  (view-generator/generate-view-file { :controller controller-name, :action action-name, :content nil, :silent true })
  (function)
  (view-destroyer/destroy-all-dependencies controller-name action-name true))
        
(use-fixtures :once setup-all)

(deftest test-find-views-directory
  (test-directory (find-views-directory) "views"))

(deftest test-view-files
  (doseq [view-file (view-files)]
    (is (.isFile view-file))
    (is (.endsWith (.getName view-file) ".clj"))))

(deftest test-find-controller-directory
  (test-directory (find-controller-directory (find-views-directory) controller-name) controller-name)
  (test-directory (find-controller-directory controller-name) controller-name)
  (is (nil? (find-controller-directory nil))))
  
(deftest test-find-view-file
  (let [controller-directory (find-controller-directory controller-name)]
    (test-file 
      (find-view-file controller-directory action-name) 
      (str action-name ".clj"))
    (is (nil? (find-view-file controller-directory nil)))
    (is (nil? (find-view-file nil action-name)))
    (is (nil? (find-view-file nil nil)))))
    
(deftest test-load-view
  (load-view { :controller controller-name
               :action action-name }))

(deftest test-request-view-namespace
  (is (= (str "views." controller-name "." action-name) 
         (request-view-namespace { :controller controller-name
                                   :action action-name })))
  (is (= "views.test-foo.show-foo" 
         (request-view-namespace { :controller "test-foo"
                                   :action "show-foo" })))
  (is (= "views.test-foo.show-foo" 
         (request-view-namespace { :controller "test_foo"
                                   :action "show_foo" })))
  (is (nil? (request-view-namespace nil))))
  
(deftest test-view-namespace
  (is (nil? (view-namespace nil)))
  (is (= (str "views." controller-name "." action-name) 
         (view-namespace (new File (find-views-directory) (str controller-name "/" action-name ".clj")))))
  (is (= (str "views.my-controller.action-view") 
         (view-namespace (new File (find-views-directory) "my_controller/action_view.clj")))))

(deftest test-all-view-namespaces
  (doseq [view-namespace (all-view-namespaces)]
    (is view-namespace)
    (is (symbol? view-namespace))))
  
(deftest test-merge-url-for-params
  (is (= 
    { :controller "hello", :action "edit", :params { :id 0 } } 
    (merge-url-for-params 
      { :controller "hello", :action "show", :params { :id 1 } } 
      { :action "edit", :params { :id 0 } })))
  (is (= 
    { :controller "hello", :action "edit" } 
    (merge-url-for-params 
      { :controller "hello", :action "show", :params { :id 1 } } 
      { :action "edit" })))
  (is (= 
    { :controller "hello", :action "edit", :id 0 } 
    (merge-url-for-params 
      { :controller "hello", :action "show", :params { :id 1, :text "blah" } } 
      { :action "edit", :id 0 }))))

(deftest test-url-for
  (is (= "/hello/show" (url-for { :controller "hello", :action "show" })))
  (is (= "/hello/show/1" (url-for { :controller "hello", :action "show", :params { :id 1 }})))
  (is (= "/hello/show/1" (url-for { :controller "hello", :action "show", :params { :id { :id 1 } } })))
  (is (= "/hello/show/#message" (url-for { :controller "hello", :action "show", :anchor "message"})))
  (is (= "/hello/show/1/#message" (url-for { :controller "hello", :action "show", :params { :id 1 }, :anchor "message"})))
  (is (= "/hello/show/1" (url-for { :controller "hello", :action "show" } { :params { :id 1 } })))
  (is (= "/hello/show/1" (url-for { :controller "hello", :action "add" } { :action "show", :params { :id 1 } })))
  (let [params { :controller "hello", :action "show", :params { :id 1 } }]
    (is (= "http://localhost/hello/show/1" (url-for { :request { :server-name "localhost" } } params)))
    (is (= "http://localhost:8080/hello/show/1" (url-for { :request { :server-name "localhost" :server-port 8080 } } params)))
    (is (= "ftp://localhost/hello/show/1" (url-for { :request { :server-name "localhost" :scheme :ftp } } params)))
    (is (= "http://localhost:8080/hello/show/1" (url-for { :request { :server-name "localhost" } } (merge params { :port 8080}))))
    (is (= "http://foo:bar@localhost/hello/show/1" (url-for { :request { :server-name "localhost" } } (merge params { :user "foo", :password "bar"})))))
  (let [params { :controller "hello", :action "show", :params { :id 1, :session-id "blah" } }]
    (binding [session-config/use-session-cookie false]
      (is (= "http://localhost/hello/show/1?session-id=blah" (url-for { :request { :server-name "localhost" } } params))))))