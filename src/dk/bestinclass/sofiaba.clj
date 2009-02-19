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

(ns dk.bestinclass.sofiaba
  (:gen-class
   :extends      com.jme.app.BaseGame
   :main         true
   :exposes      {display   {:get getDisplay :set setDisplay}} )
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

(defn toggle-state
  [nm]
  (let [ object   (first ($get nm))
         state    (second ($get nm)) ]
    ($set nm [object (not state)])))

(defn compile-it
  []
  (set! *compile-path* "/home/lau/coding/lisp/projects/sofiaba/classes/")
  (compile 'dk.bestinclass.sofiaba)
  (in-ns 'dk.bestinclass.sofiaba))

;========== MISC: TOP - BASEGAME: START


(defn -update
  [this interpolation]
  (let [ timer    ($get :timer)
         input    ($get :input)
         rootNode ($get :rootNode)
         tpf      (.getTimePerFrame timer) ]
    (.update timer)
    (.. input (update tpf))
    (.. rootNode (updateGeometricState tpf true))
    (when (.. KeyBindingManager (getKeyBindingManager) (isValidCommand "exit"))
      (.finish this)) ; This needs to be unset, otherwise SLIME requires a restart
    (when (.. KeyBindingManager (getKeyBindingManager) (isValidCommand "toggle_wire"))
      (.. (first ($get :wireState)) (setEnabled (second ($get :wireState))))
      (toggle-state :wireState)
      (.updateRenderState ($get :rootNode)))))

(defn -render
  [this interpolation]
  (.. ($get :display) (getRenderer) (clearBuffers))
  (.. ($get :display) (getRenderer) (draw ($get :rootNode))))

(defn -initSystem
  [this]
  ($set :settings (. app (getNewSettings)))
  (let [ settings   ($get :settings)
         screen     (struct-map screen :width       (.getWidth      settings)
                                       :height      (.getHeight     settings)
                                       :depth       (.getDepth      settings)
                                       :freq        (.getFreq       settings)
                                       :fullscreen? (.getFullscreen settings)) 
         display    (.. DisplaySystem (getDisplaySystem (.getRenderer settings ))) ]
    (.. this (setDisplay display))
    ($set :window
          (. display (createWindow (:width screen) (:height screen) (:depth screen)
                                   (:freq  screen) (:fullscreen? screen))))
    (.. display (getRenderer) (setBackgroundColor ColorRGBA/black))
    ($set :camera (.. display (getRenderer) (createCamera (:width  screen) (:height screen))))
    (.setFrustumPerspective ($get :camera) (float 45.0) (float (/ 640 480)) (float 1.0) (float  1000.0))    
    (let [ cam   ($get :camera)
           loc   (Vector3f. (float 0)    (float 0)   (float 25.0))
           left  (Vector3f. (float -1.0) (float 0)   (float 0))
           up    (Vector3f. (float 0)    (float 1.0) (float 0.0))
           dir   (Vector3f. (float 0)    (float 0)   (float -1.0))
           input (FirstPersonHandler. cam) ]
      (. cam (setFrame loc left up dir))
      (. cam  update)
      (.. input (getKeyboardLookHandler) (setActionSpeed (float 10.0)))
      (.. input (getMouseLookHandler)    (setActionSpeed (float  1.0)))
      (.. display (getRenderer) (setCamera ($get :camera)))
      (.. KeyBindingManager (getKeyBindingManager) (set "exit" KeyInput/KEY_ESCAPE))
      (.. KeyBindingManager (getKeyBindingManager) (set "toggle_wire" KeyInput/KEY_T))
      ($set :input input)
      ($set :timer (Timer/getTimer))
      ($set :screen  screen)
      ($set :display display))))


(defn -initGame
  [this]
  ($set :rootNode  (Node.   "rootNode"))
  ($set :sphere    (Sphere. "Sphere" 30 30 25))
  ($set :ts        (.. ($get :display) (getRenderer) (createTextureState)))
  ($set :wireState [(.. ($get :display) (getRenderer) (createWireframeState)) false])
  (let [ pointLight (PointLight.)
         lightState (.. ($get :display) (getRenderer) (createLightState)) ]
    (doto ($get :ts)
      (.setEnabled true))
    (doto (first ($get :wireState))
      (.setEnabled false)) ; This is ugly, because it mathced the $set a couple of lines above
    (doto ($get :sphere)
      (.setLocalTranslation (Vector3f. 0 0 -40))
      (.setModelBound       (BoundingBox.))
      .updateModelBound
      (.setRenderState ($get :ts)))
    (doto pointLight
      (.setDiffuse  (ColorRGBA. (float 1.0) (float 1.0) (float 1.0) (float 1.0)))
      (.setAmbient  (ColorRGBA. (float 0.5) (float 0.5) (float 0.5) (float 1.0)))
      (.setLocation (Vector3f.   100 100 100))
      (.setEnabled  true))
    (doto lightState
      (.setEnabled true)
      (.attach     pointLight))      
    (doto ($get :rootNode)
      (.setRenderState (first ($get :wireState)))
      (.setRenderState lightState)
      (.attachChild ($get :sphere))
      (.updateGeometricState (float 0.0) true)
      .updateRenderState)))                          

(defn -reinit
  [this]
  (let [ scr   ($get :screen) ]
    (. ($get :display) (reCreateWindow 640 480 (:depth       scr)
                                               (:freq        scr)
                                               (:fullscreen? scr)))))

(defn -cleanup
  [this]
  (.deleteAll ($get :ts)))

(defn -main
  []
  (doto app
    (.setConfigShowMode AbstractGame$ConfigShowMode/AlwaysShow)
    .start))