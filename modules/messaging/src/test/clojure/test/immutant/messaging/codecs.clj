;; Copyright 2008-2013 Red Hat, Inc, and individual contributors.
;; 
;; This is free software; you can redistribute it and/or modify it
;; under the terms of the GNU Lesser General Public License as
;; published by the Free Software Foundation; either version 2.1 of
;; the License, or (at your option) any later version.
;; 
;; This software is distributed in the hope that it will be useful,
;; but WITHOUT ANY WARRANTY; without even the implied warranty of
;; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
;; Lesser General Public License for more details.
;; 
;; You should have received a copy of the GNU Lesser General Public
;; License along with this software; if not, write to the Free
;; Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
;; 02110-1301 USA, or see the FSF site: http://www.fsf.org.

(ns test.immutant.messaging.codecs
  (:use [immutant.messaging.codecs])
  (:use [clojure.test]))

(defn session-mock []
  (proxy [javax.jms.Session] []
    (createTextMessage [text]
      (let [properties (java.util.HashMap.)]
        (proxy [javax.jms.TextMessage] []
          (getText [] text)
          (setStringProperty [k,v]
            (.put properties k v))
          (getStringProperty [k]
            (.get properties k)))))))

(defn bytes-message [content]
  (let [properties (java.util.HashMap.)]
    (proxy [javax.jms.BytesMessage] []
      (getBodyLength []
        (alength (.getBytes content)))
      (readBytes [bytes]
        (let [src (.getBytes content)]
          (doseq [idx (range (alength src))]
            (aset bytes idx (aget src idx)))
          (alength src)))
      (setStringProperty [k,v]
        (.put properties k v))
      (getStringProperty [k]
        (.get properties k)))))

(defn test-codec [message encoding]
  (is (= message (decode (encode (session-mock) message {:encoding encoding})))))

(deftest json-string
  (test-codec "a random text message" :json))

(deftest clojure-string
  (test-codec "a simple text message" :clojure))

(deftest clojure-date
  (test-codec (java.util.Date.) :clojure))

(deftest clojure-date-inside-hash
  (test-codec {:date (java.util.Date.)} :clojure))

(deftest clojure-date-inside-vector
  (test-codec [(java.util.Date.)] :clojure))

(deftest edn-string
  (test-codec "a simple text message" :edn))

(deftest edn-date
  (test-codec (java.util.Date.) :edn))

(deftest edn-date-inside-hash
  (test-codec {:date (java.util.Date.)} :edn))

(deftest edn-date-inside-vector
  (test-codec [(java.util.Date.)] :edn))

(deftest json-complex-hash
  (test-codec {:a "b" :c [1 2 3 {:foo 42}]} :json))

(deftest clojure-complex-hash
  (test-codec {:a "b" :c [1 2 3 {:foo 42}]} :clojure))

(deftest edn-complex-hash
  (test-codec {:a "b" :c [1 2 3 {:foo 42}]} :edn))

(deftest complex-json-encoding
  (let [message {:a "b" :c [1 2 3 {:foo 42}]}
        encoded (.getText (encode (session-mock) message {:encoding :json}))]
    (is (= encoded "{\"a\":\"b\",\"c\":[1,2,3,{\"foo\":42}]}"))))

(deftest text
  (test-codec "ham biscuit" :text))

(deftest text-as-bytes
  (let [text "ham biscuit"
        message (doto (bytes-message text)
                  (.setStringProperty encoding-header-name "text"))]
    (is (= text (decode message)))))

(deftest clojure-as-bytes
  (let [message (doto (bytes-message "\"ham biscuit\"")
                  (.setStringProperty encoding-header-name "clojure"))]
    (is (= "ham biscuit" (decode message)))))

(deftest edn-as-bytes
  (let [message (doto (bytes-message "\"ham biscuit\"")
                  (.setStringProperty encoding-header-name "edn"))]
    (is (= "ham biscuit" (decode message)))))

(deftest json-as-bytes
  (let [message (doto (bytes-message "[\"ham biscuit\"]")
                  (.setStringProperty encoding-header-name "json"))]
    (is (= ["ham biscuit"] (decode message)))))

(deftest default-to-text-if-no-encoding-given
  (is (= "ham biscuit" (decode (.createTextMessage (session-mock) "ham biscuit")))))
