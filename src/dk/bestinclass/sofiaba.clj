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
  (:import (com.jme.app                  BaseGame SimpleGame AbstractGame AbstractGame$ConfigShowMode)
           (com.jme.image                Texture)
           (com.jme.input                FirstPersonHandler InputHandler KeyBindingManager KeyInput)
           (com.jme.light                PointLight DirectionalLight)
           (com.jme.math                 Vector3f)
           (com.jme.renderer             Camera ColorRGBA)
           (com.jme.scene                Node Text Spatial Skybox)
           (com.jme.scene.shape          Box Sphere Quad)
           (com.jme.scene.state          LightState TextureState WireframeState
                                         ZBufferState ZBufferState$TestFunction)
           (com.jme.system               DisplaySystem JmeException)
           (com.jme.bounding             BoundingBox)
           (com.jme.util                 TextureManager Timer)
           (com.jme.util.geom            Debugger)
           (com.jmex.terrain             TerrainBlock)
           (com.jmex.terrain.util        MidPointHeightMap ProceduralTextureGenerator ImageBasedHeightMap)
           (com.jmex.physics             DynamicPhysicsNode StaticPhysicsNode)
           (com.jmex.physics.util.states PhysicsGameState)
           (javax.swing                  ImageIcon))
  (:load           "sofiaba/resources"
                   "sofiaba/wrappers"
                   "sofiaba/environment" ))

;========== IMPORTS: STOP - DEFINITIONS: START

(defstruct screen    :width :height :depth :freq :fullscreen?)

(def app (dk.bestinclass.sofiaba.))

(defn toggle-state
  [nm]
  (let [ object   (first ($get nm))
         state    (second ($get nm)) ]
    ($set nm [object (not state)])))

(defn compile-it
  " Personal helper function, remove from your project "
  []
  (set! *compile-path* "/home/lau/coding/lisp/projects/sofiaba/classes/")
  (compile 'dk.bestinclass.sofiaba)
  (in-ns 'dk.bestinclass.sofiaba)
  (.run #(Thread. (.start app))))

;========== MISC: STOP - BASEGAME: START

(defn updateWater
  [tpf]
  (let [  tex1   (:tex1 ($get :waterworld))
          tex2   (:tex2 ($get :waterworld)) ]
    (when (> (.getX (.getTranslation tex1)) 5000)
      (.setTranslation tex1 (Vector3f. 0 0 0)))
    (when (> (.getY (.getTranslation tex2)) 5000)
      (.setTranslation tex2 (Vector3f. 0 0 0)))
    (.setX (.getTranslation tex1) (+ (.getX (.getTranslation tex1)) (* (float 0.00004) tpf)))
    (.setY (.getTranslation tex2) (+ (.getY (.getTranslation tex2)) (* (float 0.09) tpf)))
    (doto (:quad1 ($get :waterworld))
      .updateRenderState
      (.updateGeometricState tpf true))
    (doto (:quad2 ($get :waterworld))
      .updateRenderState
      (.updateGeometricState tpf true))))

(defn actOnInput
  [this]
  (when (.. KeyBindingManager (getKeyBindingManager) (isValidCommand "nunnaba"))
    (addNunna ($get :rootnode) ($get :display))
    (.updateRenderState ($get :rootnode)))
  (when (.. KeyBindingManager (getKeyBindingManager) (isValidCommand "exit"))
    (.finish this)) ; This needs to be unset, otherwise SLIME requires a restart
  (when (.. KeyBindingManager (getKeyBindingManager) (isValidCommand "toggle_wire"))
    (.. (first ($get :wirestate)) (setEnabled (second ($get :wirestate))))
    (toggle-state :wirestate)
    (.updateRenderState ($get :rootnode))))

(defn -update
  [this interpolation]
  (let [ timer    ($get :timer)
         input    ($get :input)
         rootNode ($get :rootnode)
         tpf      (.getTimePerFrame timer) ]
    (.update timer)
    (actOnInput this)
    (.. input (update tpf))
    (. ($get :skybox) (setLocalTranslation (.getLocation ($get :camera))))
    (.. rootNode (updateGeometricState tpf true))
    (updateWater tpf)))

(defn -render
  [this interpolation]
  (.. ($get :display) (getRenderer) (clearBuffers))
  (.. ($get :display) (getRenderer) (draw ($get :rootnode))))

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
    (let [ camera (makeCamera        display screen 1 4000)
           input  (makeInputHandler  camera  600     1) ]
      (.. display (getRenderer) (setCamera camera))
      (attachCommands { :exit        KeyInput/KEY_ESCAPE
                        :toggle_wire KeyInput/KEY_T
                        :nunnaba     KeyInput/KEY_N })
      (indoctrinate
          :input   input
          :timer   (Timer/getTimer)
          :screen  screen
          :display display))))

(defn applyRenderStates
  " Apply one or more RenderStates to a node. These will be automatically updated
    using updateRenderState, so toggle individually using setEnabled "
  [node & states]
  (doseq [rstate states]
    (.setRenderState node rstate))
  (.updateRenderState node))

(defn -initGame
  [this]
  (indoctrinate
      :rootnode      (Node.   "rootNode")
      :ts            (.. ($get :display) (getRenderer) (createTextureState))
      :wirestate     [(.. ($get :display) (getRenderer) (createWireframeState)) false]
      :skybox        (makeSkybox)
      :waterworld    (buildWater ($get :display))
      :primarylight  (makeDirectedLight [1 1 1 1] [0.5 0.5 0.5 1] [0 -1 0])
      :terrainblock  (buildTerrain)
      :zbuffer       (makeZBuffer ZBufferState$TestFunction/LessThanOrEqualTo))
  (. ($get :ts) (setEnabled true))
  (. (first ($get :wirestate)) (setEnabled false)) ; This is ugly, because it matched
                                                     ; the $set a couple of lines above
  (addToRoot (:quad1 ($get :waterworld)) (:quad2 ($get :waterworld))
             ($get :terrainblock) ($get :skybox))
  (applyRenderStates ($get :rootnode) (first ($get :wirestate)) ($get :primarylight) ($get :zbuffer)))

(defn -reinit
  [this]
  (let [ scr   ($get :screen) ]
    (. ($get :display)
       (reCreateWindow (:width scr) (:height scr) (:depth scr) (:freq scr) (:fullscreen? scr)))))

(defn -cleanup
  [this]
  (.deleteAll ($get :ts)))

(defn -main
  []
  (doto app
    (.setConfigShowMode AbstractGame$ConfigShowMode/AlwaysShow (get-resource :logo))
    .start))

