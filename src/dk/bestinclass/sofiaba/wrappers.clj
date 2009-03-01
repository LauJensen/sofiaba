(in-ns 'dk.bestinclass.sofiaba)

(import '(com.jme.scene       Skybox$Face Spatial$LightCombineMode)
        '(com.jme.image       Texture$ApplyMode Texture$CombinerFunctionRGB
                              Texture$CombinerSource Texture$CombinerOperandRGB
                              Texture$CombinerScale Texture$WrapMode )
        '(com.jme.scene.state BlendState BlendState$SourceFunction
                              BlendState$DestinationFunction BlendState$TestFunction)
        '(com.jme.animation   SpatialTransformer)
        '(com.jme.math        Quaternion FastMath)
        '(com.jmex.terrain    TerrainPage))

(def BilNoMipMap com.jme.image.Texture$MinificationFilter/BilinearNoMipMaps)
(def Bil         com.jme.image.Texture$MagnificationFilter/Bilinear)

;========= GLOBALS

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

(defn indoctrinate
  " Takes a map of global definitions and stores them using $set "
  [& args]
  (let [map-of-candidates (apply hash-map args)]
    (doseq [this-key (keys map-of-candidates)]
      ($set this-key (this-key map-of-candidates)))))

;======= INPUT HANDLING

(defn makeInputHandler
  " Spawns a new Inputhandler from a given camera, input handler is returned "
  [camera keyboardspeed mousespeed]
  (let [ input       (FirstPersonHandler. camera) ]
    (.setActionSpeed (.getKeyboardLookHandler input)  keyboardspeed)
    (.setActionSpeed (.getMouseLookHandler    input)  mousespeed)
    input))
  
(defn attachCommands
  " Takes a hash-map of commands as keys and KEYS as the values, ex.
     (attachCommands {:exit KeyInput/KEY_ESCAPE}) "
  [map-of-commands]
  (doseq [key (keys map-of-commands)]
     (.. KeyBindingManager (getKeyBindingManager) (set (name key) (key map-of-commands)))))

 ;======= LIGHTS

(defmacro mapmac
  [t f c]
  (lazy-cons t (map (eval f) c)))   ;; TODO: UPDATE TO LATEST REV OF CLOJURE

(defmacro >RGBA
  [vec]
  `(mapmac ColorRGBA. (fn [x#] (float x#)) ~vec))

(defmacro >Vec3f
  [vec]
  `(mapmac Vector3f. (fn [x#] (float x#)) ~vec))

(defn makeDirectedLight
  " Returns a LightState containing only a DirectionalLight "
  [diffuse ambient direction]
  (if-let [ display ($get :display) ]
    (let [lightState    (.. display (getRenderer) (createLightState))
          light         (DirectionalLight.) ]
      (doto light
        (.setDiffuse     (eval `(>RGBA  ~diffuse)))
        (.setAmbient     (eval `(>RGBA  ~ambient)))
        (.setDirection   (eval `(>Vec3f ~direction)))
        (.setEnabled     true))
      (doto lightState
        .detachAll
        (.attach         light)
        (.setEnabled     true)))
    (throw (Exception. "Global key :display must be set before calling"))))

 ;======= CAMERA

(defn makeCamera
  " Makes a camera for DISPLAYs renderer, sets the global :camera and returns the camera object "
  [display settings near far location]
  ($set :camera (.. display (getRenderer) (createCamera (.getWidth settings) (.getHeight settings))))
  (let [ cam    ($get :camera)
         loc    (eval `(>Vec3f ~location))
         left   (Vector3f.   -1.0     0      0)
         up     (Vector3f.    0       1      0)
         dir    (Vector3f.    0       0     -1)
         ratio  (float (/ (.getWidth settings) (.getHeight settings))) ]
    (.setFrustum            cam near far     50 50 50 50)
    (.setFrustumPerspective cam 45   ratio near far)
    (.setFrame              cam loc left up dir)
    (.update                cam)
    cam))

 ;======= ZBuffer

(defn makeZBuffer
  [func]
  (if-let [display  ($get :display)]
    (let  [zBuf     (.. display (getRenderer) (createZBufferState))]
      (doto zBuf
        (.setEnabled true)
        (.setFunction func)))
    (throw (Exception. "Global key :display must be set before calling"))))

;======= ENVIRONMENT

(defn makeFogState
  [color start end]
  (if-let [ display   ($get :display) ]
    (let  [ fs        (.. display (getRenderer) (createFogState)) ]
      (doto fs
        (.setDensity         0.5)
        (.setEnabled         true)
        (.setColor           color)
        (.setEnd             end)
        (.setStart           start)
        (.setDensityFunction com.jme.scene.state.FogState$DensityFunction/Linear)
        (.setQuality         com.jme.scene.state.FogState$Quality/PerVertex)))
    (throw (Exception. "Global key :display must be set before calling"))))

;======= RESOURCES: STOP - HELPERS START

(defn addToRoot
  " Helper for adding multiple objects to the root node "
  [& children]
  (if-let [rootNode  ($get :rootnode)]
    (doseq [child children]
      (if (keyword? child)
      (.attachChild rootNode ($get child))
      (.attachChild rootNode child)))      
    (throw (Exception. "Global key :rootnode must be set before calling"))))

(defn applyRenderStates
  " Apply one or more RenderStates to a node. These will be automatically updated
    using updateRenderState, so toggle individually using setEnabled "
  [node & states]
  (doseq [rstate states]
    (.setRenderState node rstate))
  (.updateRenderState node))

(defn >Image
  " A helper to convert a file to an Image, via ImageIcon "
  [resource-key]
  (if (keyword? resource-key)
    (.getImage (ImageIcon. (get-resource resource-key)))
    (.getImage (.getImageIcon resource-key)))) ; Presumed ProceduralTextureGenerator

(defn flipQuad
  [s]
  (let [rotQuad   (Quaternion.) ]
    (.fromAngleNormalAxis rotQuad (* FastMath/PI (float 0.5)) Vector3f/UNIT_X)
    (.setLocalRotation s rotQuad)))

(defn setTexture
  [obj tex]
  (if-let [display   ($get :display) ]
    (let [texState  (.. display (getRenderer) (createTextureState))
          texture   (TextureManager/loadTexture (>Image tex) BilNoMipMap Bil true) ]
      (.setTexture texState texture 0)
      (.setRenderState obj texState))
    (throw (Exception. "Global key :display must be set before calling"))))

(defn makeLabel
  [name content location]
  (let [ label  (Text/createDefaultTextLabel name content) ]
    (.setLocalTranslation label (eval `(>Vec3f ~location)))
    label))


;;=== EXPERIMENTAL EXPERIMENTAL EXPERIMENTAL

(defn makeSwarm
  [offset]
  (let [ ppoints  (ParticleFactory/buildPointParticles "Swarm" 200)
         Zstate   (.. ($get :display) (getRenderer) (createZBufferState)) ]
    (.setEnabled Zstate true)
    (doto ppoints
      (.setPointSize         1.5) ;arg
      (.setAntialiased       true)
      (.setEmissionDirection offset) ; arg
      (.setOriginOffset      offset)
      (.setInitialVelocity   (float 0.4))
      (.setStartSize         (float 3))
      (.setEndSize           (float 1))
      (.setMinimumLifeTime    100)
      (.setMaximumLifeTime    1000)
      (.setStartColor        (ColorRGBA. 0 0 1 1))
      (.setEndColor          (ColorRGBA. 0 1 1 0))
      (.setMaximumAngle      (* 360 FastMath/DEG_TO_RAD))
      (.warmUp               220)
      (.setRenderState       Zstate)
      (.setModelBound        (BoundingSphere.))
      .updateModelBound)
    (let [swarm   (SwarmInfluence. (.getWorldTranslation ppoints) 500) ]
      (doto swarm
        (.setMaxSpeed  (float 2))
        (.setSpeedBump (float 5))
        (.setTurnSpeed (* FastMath/DEG_TO_RAD 360)))
      (.. ppoints (getParticleController) (setControlFlow false))
;      (.addInfluence ppoints swarm)
      ppoints)))

(defn isCollided?
  [obj1 obj2]
   (and (= (int (.x obj1)) (int (.x obj2)))
        (= (int (.y obj1)) (int (.y obj2)))              
        (= (int (.z obj1)) (int (.z obj2)))))

(defn strvec
  [vec3f]
  (format "[x: %d|y: %d|z: %d]" (int (.x vec3f)) (int (.y vec3f)) (int (.z vec3f))))

(defn updateSwarm
  [fps]
  (when (isCollided? ($get :currentpos) ($get :nextpos))
;    (println "COLLISION")
    ($set :nextpos (Vector3f.
                    (float (- (* (Math/random) 50) 0))
                    (float (- (* (Math/random) 50) 0))
                    (float (- (* (Math/random) 50) 0)))))
  (let [currentPos  ($get :currentpos)
        nextPos     ($get :nextpos)
        fps         (/ fps 2) ]
  ;  (println (str (strvec ($get :currentpos)) " - " (strvec ($get :nextpos))))
    ($set :currentpos (Vector3f.
                      (/ (Math/abs (- (.x ($get :currentpos)) (.x ($get :nextpos)))) 8)
                      (/ (Math/abs (- (.y ($get :currentpos)) (.y ($get :nextpos)))) 8)
                      (/ (Math/abs (- (.z ($get :currentpos)) (.z ($get :nextpos)))) 8)))
 ;   (println (str (strvec ($get :currentpos)) " - " (strvec ($get :nextpos))))
  (.setOriginOffset ($get :swarm) ($get :currentpos))))


