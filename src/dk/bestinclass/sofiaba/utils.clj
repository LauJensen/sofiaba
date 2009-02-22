;; Copyright (c) 2008,2009 Lau B. Jensen <lau.jensen {at} bestinclass.dk
;;                         
;; All rights reserved.
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file LICENSE.txt at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by the
;; terms of this license. You must not remove this notice, or any other, from
;; this software.

(clojure.core/in-ns 'dk.bestinclass.sofiaba)

;========= GLOBALS: START

(def *globals* (ref (hash-map)))

(defn $get
  " Retrieves the value in keyword k - Global vars "
  [k]
  (@*globals* k))

(defn $set
  " Set the keyword k to a value - Global vars "
  [k value]
  (dosync
   (alter *globals* assoc k value)))

;======= GLOBALS: STOP - RESOURCES: START

(def *resources*
     {:logo       "res/logo.png"
      
      :grass      "res/textures/grass.jpg"
      :dirt       "res/textures/dirt.jpg"
      
      :north      "res/skybox/north.jpg"
      :south      "res/skybox/south.jpg"
      :west       "res/skybox/west.jpg"
      :east       "res/skybox/east.jpg"
      :top        "res/skybox/top.jpg"

      :detailmap  "res/textures/detail.jpg" } )
     

(defn get-resource-uri
  [res]
  (if-let [ uri  (. (.. Class (forName "dk.bestinclass.sofiaba") (getClassLoader))
                    (findResource (str "dk/bestinclass/" res)))]
          uri
          (throw (Exception. (format "Resource not found: %s" res)))))

(defn get-resource
  [key]
  (get-resource-uri (*resources* key)))

;======= RESOURCES: STOP -