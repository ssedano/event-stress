(ns event-stress.core
  (:gen-class)
  (:require [langohr.core :as rmq]
            [langohr.channel :as lch]
            [langohr.basic :as lb]
            [clj-time.core :as t]
            [clj-time.coerce :as r])
  (:import com.abiquo.event.model.enumerations.Severity
           com.abiquo.event.model.enumerations.EntityAction)
  (:use [clojure.string :only [lower-case]]))

(def entities (map #(.getSimpleName %) (filter #(not (.isEnum %)) (.getDeclaredClasses EntityAction))))

(def entityClass (map #(Class/forName (.getName %)) (filter #(not (.isEnum %)) (.getDeclaredClasses EntityAction))))

(defn user [] (rand-nth ["SYSTEM", (str "/admin/enterprises/1/users/" (rand-int 10))]))

(defn severity [] (rand-nth (Severity/values)))

(defn entity [] (rand-nth entities))

(defn action [e]
  (let [class-name (str "com.abiquo.event.model.enumerations.EntityAction$" e)
        clazz (Class/forName class-name)
        fields (map #(.getName %) (.getDeclaredFields clazz))]
      (rand-nth fields)))

(defn entity-id [en]
  (str "/admin/" (lower-case en) "/" (rand-int 10)))

(defn event [] 
  (let [e (entity)
        a (action e)]
    (str "{\"timestamp\":" (r/to-long (t/now)) ",\"user\":\"" (user) "\",\"severity\":\"" (severity) "\",\"source\":\"ABIQUO_SERVER\",\"action\":\"" a "\",\"type\":\"" e "\",\"entityIdentifier\":\"" (entity-id e) "\",\"details\":{\"detail\":[]}}")))

(defn stress [n]
  (let [ch (lch/open (rmq/connect))]
    (repeatedly n  #(lb/publish ch "abiquo.tracer" "abiquo.tracer.traces" (event) :content-type "text/plain"))))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  {:pre [(= (count args) 1) (number? (read-string (first args)))]}
  ;; work around dangerous default behaviour in Clojure
  (alter-var-root #'*read-eval* (constantly false))
  (let [n (read-string (first args))]
    (stress n)
    (System/exit 0)))

