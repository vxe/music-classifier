
(ns music-classifier.core
  (:require [clj-http.client])
  (:use [com.rpl.specter]
        [clojure.java.shell :only [sh]])
  (:import java.util.Base64))
