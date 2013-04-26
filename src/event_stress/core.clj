(ns event-stress.core
  (:gen-class)
  (:require [langohr.core :as rmq]
            [langohr.exchange :as le]
            [langohr.channel :as lch]
            [langohr.basic :as lb]
            [langohr.queue :as lq]
            [clj-time.core :as t]
            [clj-time.coerce :as r])
  (:import com.abiquo.event.model.enumerations.Severity
           com.abiquo.event.model.enumerations.EntityAction)
  (:use [clojure.string :only [lower-case capitalize]]))


(def exchange "abiquo.tracer")

(def queue "abiquo.tracer.traces")

(def entities (map #(.getSimpleName %) (remove #(or (.isEnum %) (= (.getSimpleName %) "") (= (.getSimpleName %) "Action")) (.getDeclaredClasses EntityAction))))

(def entityClass (map #(Class/forName (.getName %)) (remove #(.isEnum %) (.getDeclaredClasses EntityAction))))

(def package "com.abiquo.event.model.enumerations.EntityAction$")

(defn user [] (rand-nth ["SYSTEM", (str "/admin/enterprises/1/users/" (rand-int 10))]))

(defn severity [] (rand-nth (Severity/values)))

(defn action [e]
  (let [class-name (str package e)
        clazz (Class/forName class-name)
        fields (map #(.getName %) (.getDeclaredFields clazz))]
    (rand-nth fields)))

(defn entity-id [en]
  (str "/admin/" (lower-case en) "/" (rand-int 10)))


(defn details [sev ent act]
  (let [m (str "get" (capitalize (lower-case sev)) "keys")
        class-name (str package ent)
        clazz (Class/forName class-name)
        field (. clazz getDeclaredField act)
        method (. field get m)
        fields (. (. method getClass) getDeclaredMethod m nil)
        k (map #(.name %) (. fields invoke method (make-array Object 0)))]
    (vec (interleave (take (count k) (repeatedly #(str (rand-int 100)))) (repeat (count k) ",")))))

(defn event [] 
  (let [e (rand-nth entities)
        a (action e)
        s (severity)
        d (details s e a)]
    (str "{\"timestamp\":" (r/to-long (t/now)) ",\"user\":\"" (user) "\", \"enterprise\":\"" (user) "\",\"severity\":\"" s "\",\"source\":\"ABIQUO_SERVER\",\"action\":\"" a "\",\"type\":\"" e "\",\"entityIdentifier\":\"" (entity-id e) "\",\"details\":{\"detail\": " d "}}")))

(defn stress [n]
  (let [conn (rmq/connect)
        ch (lch/open conn)]
    (dotimes [i n]
      (lb/publish ch exchange queue (event) :content-type "text/plain"))))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  {:pre [(= (count args) 1) (number? (read-string (first args)))]}
  ;; work around dangerous default behaviour in Clojure
  (alter-var-root #'*read-eval* (constantly false))
  (let [n (read-string (first args))]
    (time
      (stress n))
    (System/exit 0)))
  (event)
