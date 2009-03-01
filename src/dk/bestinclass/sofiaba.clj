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
           (com.jme.scene                Node Text Spatial Skybox Text)
           (com.jme.scene.shape          Box Sphere Quad)
           (com.jme.scene.state          LightState TextureState WireframeState
                                         ZBufferState ZBufferState$TestFunction)
           (com.jme.system               DisplaySystem JmeException)
           (com.jme.bounding             BoundingBox BoundingSphere)
           (com.jme.util                 TextureManager Timer)
           (com.jme.util.geom            Debugger)
           (com.jmex.terrain             TerrainBlock)
           (com.jmex.terrain.util        MidPointHeightMap ProceduralTextureGenerator ImageBasedHeightMap)
           (com.jmex.effects.particles    ParticleFactory ParticlePoints SwarmInfluence)
;           (com.jmex.physics             DynamicPhysicsNode StaticPhysicsNode)
;           (com.jmex.physics.util.states PhysicsGameState)
           (javax.swing                  ImageIcon))
  (:load           "sofiaba/resources"
                   "sofiaba/wrappers"
                   "sofiaba/environment" ))

;========== DEFINITION

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
  (in-ns 'dk.bestinclass.sofiaba))
;  (.run #(Thread. (.start app))))

;========== BASEGAME

(defn actOnInput
  [this]
  (when (.. KeyBindingManager (getKeyBindingManager) (isValidCommand "nunnaba"))
    (addNunna ($get :rootnode) ($get :display))
    (.updateRenderState ($get :rbootnode)))
  (when (.. KeyBindingManager (getKeyBindingManager) (isValidCommand "exit"))
    (.finish this)) ; This needs to be unset, otherwise SLIME requires a restart
  (when (.. KeyBindingManager (getKeyBindingManager) (isValidCommand "swarm"))
      (indoctrinate
          :currentpos (Vector3f. 237 294 239) ;On top of 'mountain-peak'
          :nextpos    (Vector3f. 324 114 433)) ;Foot of mountain
      ($set :swarm      (makeSwarm ($get :currentpos)))
      (addToRoot ($get :swarm)))
  (when (.. KeyBindingManager (getKeyBindingManager) (isValidCommand "toggle_wire"))
    (.. (first ($get :wirestate)) (setEnabled (second ($get :wirestate))))
    (toggle-state :wirestate)
    (.updateRenderState ($get :rootnode))))

(defn -update
  [this interpolation]
  (let [ timer     ($get :timer)
         input     ($get :input)
         rootNode  ($get :rootnode)
         tpf       (.getTimePerFrame timer)
         cameraLoc (.getLocation ($get :camera)) ]
    (.update timer)
    (actOnInput this)
    (updateWater tpf)
;    (when-not (nil? ($get :swarm))
 ;           (updateSwarm (.getFrameRate timer)))
    (.print ($get :fpslabel) (str (format "Camera (%d,%d,%d) "   (int (.x cameraLoc))
                                                                 (int (.y cameraLoc))
                                                                 (int (.z cameraLoc)))
                                  "  FPS: " (int (.getFrameRate timer))))
    (.update input tpf)
    (. ($get :skybox) (setLocalTranslation (.getLocation ($get :camera))))
    (.. rootNode (updateGeometricState tpf true))))


(defn -render
  [this interpolation]
  (.. ($get :display) (getRenderer) (clearBuffers))
  (.. ($get :display) (getRenderer) (draw ($get :rootnode))))

(defn -initSystem
  [this]
  ($set :settings (. app (getNewSettings)))
  (let [ settings   ($get :settings)
         display    (.. DisplaySystem (getDisplaySystem (.getRenderer settings )))
         window     (. display (createWindow (.getWidth settings) (.getHeight settings)
                                             (.getDepth settings) (.getFreq   settings)
                                             (.getFullscreen settings)))]
    (attachCommands { :exit        KeyInput/KEY_ESCAPE
                      :toggle_wire KeyInput/KEY_T
                      :nunnaba     KeyInput/KEY_N
                      :swarm       KeyInput/KEY_P })
    (indoctrinate
          :camera  (makeCamera       display settings 1 6000 [300 150 1000])
          :input   (makeInputHandler ($get :camera) 600  1)
          :timer   (Timer/getTimer)
          :screen  settings
          :window  window
          :display display)
        (.. this     (setDisplay display))
    (.. display (getRenderer) (setCamera ($get :camera)))
    (.. display  (getRenderer) (setBackgroundColor ColorRGBA/black))))

(defn -initGame
  [this]
  (indoctrinate
      :rootnode      (Node.   "rootNode")
      :ts            (.. ($get :display) (getRenderer) (createTextureState))
      :wirestate     [(.. ($get :display) (getRenderer) (createWireframeState)) false]
      :skybox        (makeSkybox)
      :fpslabel      (makeLabel "Fps Label" "Timer" [1 10 0])
      :fogstate      (makeFogState ColorRGBA/white 700 4000)
      :waterworld    (buildWater ($get :display))
      :primarylight  (makeDirectedLight [1 1 1 1] [0.5 0.5 0.5 1] [0 -1 0])
      :terrainblock  (buildTerrain)
      :zbuffer       (makeZBuffer ZBufferState$TestFunction/LessThanOrEqualTo)
      :beach         (makeBeach))
  (. ($get :ts) (setEnabled true))
  (. (first ($get :wirestate)) (setEnabled false)) ; This is ugly (see :wirestate init above)
  (addToRoot (:quad1 ($get :waterworld)) (:quad2 ($get :waterworld))
              :terrainblock :skybox :beach :fpslabel)
  (applyRenderStates ($get :rootnode) (first ($get :wirestate)) ($get :primarylight) ($get :zbuffer)
                     ($get :fogstate)))

(defn -reinit
  [this]
  (let [ scr   ($get :settings) ]
    (. ($get :display)
       (reCreateWindow (.getWidth scr) (.getHeight scr)
                       (.getDepth scr) (.getFreq   scr) (.getFullscreen? scr)))))

(defn -cleanup
  [this]
  (.deleteAll ($get :ts)))

(defn -main
  []
  (doto app
    (.setConfigShowMode AbstractGame$ConfigShowMode/NeverShow (get-resource :logo))
    .start))

