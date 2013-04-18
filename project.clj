(defproject event-stress "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [com.novemberain/langohr "1.0.0-beta13"]
                 [clj-time "0.5.0"]
                 [com.abiquo/event-model-transport "1.0-SNAPSHOT"]]
  :jvm-opts ["-XX:+TieredCompilation" "-XX:TieredStopAtLevel=1"]
  :main event-stress.core)
