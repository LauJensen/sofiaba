(ns dk.bestinclass.sofiaba
  (:gen-class
   :extends      com.jme.app.BaseGame
   :main         true
   :exposes      {display   {:get getDisplay :set setDisplay}})
  (:import (com.jme.app         BaseGame SimpleGame AbstractGame AbstractGame$ConfigShowMode)
           (com.jme.image       Texture)
           (com.jme.input       FirstPersonHandler InputHandler KeyBindingManager KeyInput)
           (com.jme.light       PointLight)
           (com.jme.math        Vector3f)
           (com.jme.renderer    Camera ColorRGBA)
           (com.jme.scene       Node Text Spatial)
           (com.jme.scene.shape Box Sphere)
           (com.jme.scene.state LightState TextureState WireframeState ZBufferState)
           (com.jme.system      DisplaySystem JmeException)
           (com.jme.bounding    BoundingBox)
           (com.jme.util        TextureManager Timer)
           (com.jme.util.geom   Debugger)))


(println "Imports completed")

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

;========== GLOBALS: STOP - MISC: START

(defstruct screen    :width :height :depth :freq :fullscreen?)

(def app (dk.bestinclass.sofiaba.))

(defn compile-it
  []
  (set! *compile-path* "/home/lau/coding/lisp/projects/sofiaba/classes/")
  (compile 'dk.bestinclass.sofiaba)
  (in-ns 'dk.bestinclass.sofiaba))

;========== MISC: TOP - BASEGAME: START


(defn -update
  [this interpolation]
  (.update ($get :timer))
  (if (.. KeyBindingManager (getKeyBindingManager) (isValidCommand "exit"))
    (.finish this)))
  

(defn -render
  [this interpolation]
  (.. ($get :display) (getRenderer) (clearBuffers))
  (.. ($get :display) (getRenderer) (draw ($get :rootNode))))


(defn -initSystem
  [this]
  (println "== initSystem running")
  ($set :settings (. app (getNewSettings)))
  ($set :screen (struct-map screen :width       (.getWidth      ($get :settings))
                                   :height      (.getHeight     ($get :settings))
                                   :depth       (.getDepth      ($get :settings))
                                   :freq        (.getFreq       ($get :settings))
                                   :fullscreen? (.getFullscreen ($get :settings))))
  ($set :display (.. DisplaySystem (getDisplaySystem (.getRenderer ($get :settings) ))))
  (.. this (setDisplay ($get :display)))
  ($set :window
        (. ($get :display) (createWindow (:width       ($get :screen))
                                         (:height      ($get :screen))
                                         (:depth       ($get :screen))
                                         (:freq        ($get :screen))
                                         (:fullscreen? ($get :screen)))))
  (.. ($get :display) (getRenderer) (setBackgroundColor ColorRGBA/black))
  ($set :camera (.. ($get :display) (getRenderer) (createCamera (:width  ($get :screen))
                                                                (:height ($get :screen)))))
  (.setFrustumPerspective ($get :camera)     (float 45.0)
                                             (float (/ 640 480))
                                             (float 1.0)
                                             (float  1000.0))
  (let [ loc  (Vector3f. (float 0)    (float 0)   (float 25.0))
         left (Vector3f. (float -1.0) (float 0)   (float 0))
         up   (Vector3f. (float 0)    (float 1.0) (float 0.0))
         dir  (Vector3f. (float 0)    (float 0)   (float -1.0)) ]
    (. ($get :camera) (setFrame loc left up dir))
    (. ($get :camera)  update))
  (.. KeyBindingManager (getKeyBindingManager) (set "exit" KeyInput/KEY_ESCAPE))
  ($set :timer (Timer/getTimer))
  (.. ($get :display) (getRenderer) (setCamera ($get :camera))))


(defn -initGame
  [this]
  (println "== initGame running")
  ($set :rootNode    (Node.   "rootNode"))
  ($set :sphere   (Sphere. "Sphere" 30 30 25))
  ($set :ts (.. ($get :display) (getRenderer) (createTextureState)))
  (println "== initGame: DOTO :ts")
  (doto ($get :ts)
    (.setEnabled true))
  (println "== initGame: DOTO sphere")
  (doto ($get :sphere)
    (.setLocalTranslation (Vector3f. 0 0 -40))
    (.setModelBound       (BoundingBox.))
    .updateModelBound
    (.setRenderState ($get :ts)))
  (println "== initGame: DOTO scene")
  (doto ($get :rootNode)
    (.attachChild ($get :sphere))
    (.updateGeometricState (float 0.0) true)
    .updateRenderState)
  (println "== initGame: DONE"))
                          

(defn -reinit
  [this]
  (println "== reinit running")
  (let [ scr   ($get :screen) ]
    (. ($get :display) (reCreateWindow 640 480 (:depth       scr)
                                               (:freq        scr)
                                               (:fullscreen? scr)))))

(defn -cleanup
  [this]
  (println "== cleanup")
  (.deleteAll ($get :ts)))

(defn -main
  []
  (println "== main running")
  (doto app
    (.setConfigShowMode AbstractGame$ConfigShowMode/AlwaysShow)
    .start))

