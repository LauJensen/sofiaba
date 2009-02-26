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

;======= OBJECTS: START

(defn addController
  [node]
  (let [ controller   (SpatialTransformer. 1)
         x0           (Quaternion. )
         x180         (Quaternion. )
         x360         (Quaternion. )
         z            (Vector3f. 0 0 1) ]
;    (.setObject     controller (float 0) (float -1))
    (.fromAngleAxis x0   0 z)
    (.setRotation   controller 0 0 x0)
    (.fromAngleAxis x180 (* FastMath/DEG_TO_RAD 180) z)
    (.setRotation   controller 0 2 x180)
    (.fromAngleAxis x360 (* FastMath/DEG_TO_RAD 360) z)
    (.setRotation   controller 0 4 x360)
    (.interpolateMissing controller)
    (.addController node controller)))

(defn addNunna
  [node display]
  (let [ sphere       (Sphere. "Nunna Sphere" 32 32 80)
         textureState (.. display (getRenderer) (createTextureState))
         texture      (TextureManager/loadTexture (>Image :nunna) BilNoMipMap Bil true)]
    (doto textureState
      (.setTexture texture)
      (.setEnabled true))
    (.setRenderState sphere textureState)
    (.setLocalTranslation sphere (Vector3f. 280 800 325))
    (.updateRenderState sphere)
;    (addController node)
    ($set :nunna sphere)
    (.attachChild node sphere)))

;======= OBJETS - STOP - TERRAIN: START

(defn buildTerrain
  []
  (let [ heightMap     (ImageBasedHeightMap. (>Image :heightmap));(MidPointHeightMap. 512 (float 0.90))
         terrainScale  (Vector3f. (float 0.5) (float 0.05) (float 0.5))
         terrainBlock  (TerrainBlock. "Terrain" (.getSize heightMap) terrainScale
                                   (.getHeightMap heightMap) (Vector3f. 0 0 0)) 
         textGenerator (ProceduralTextureGenerator. heightMap) 
         textState     (.. ($get :display) (getRenderer) (createTextureState)) ]
    (doto terrainBlock
      (.setModelBound (BoundingBox.))
      .updateModelBound)
    (doto textGenerator
      (.addTexture (ImageIcon. (get-resource :water))    -128   0 128)
      (.addTexture (ImageIcon. (get-resource :dirt))        0 128 255)
      (.addTexture (ImageIcon. (get-resource :highest))   128 255 384)
      (.createTexture 256))
    (let
        [t1 (TextureManager/loadTexture (>Image textGenerator) BilNoMipMap Bil true)
         t2 (TextureManager/loadTexture (>Image :detailmap)    BilNoMipMap Bil true) ]
      (. t2 (setWrap Texture$WrapMode/MirroredRepeat))
      (doto t1
        (.setApply             Texture$ApplyMode/Combine)
        (.setCombineFuncRGB    Texture$CombinerFunctionRGB/Modulate)
        (.setCombineSrc0RGB    Texture$CombinerSource/CurrentTexture) ;;???
        (.setCombineOp0RGB     Texture$CombinerOperandRGB/SourceColor)
        (.setCombineSrc1RGB    Texture$CombinerSource/PrimaryColor)
        (.setCombineOp1RGB     Texture$CombinerOperandRGB/SourceColor)
        (.setCombineScaleRGB   Texture$CombinerScale/One))
     (doto t2
        (.setApply             Texture$ApplyMode/Combine)
        (.setCombineFuncRGB    Texture$CombinerFunctionRGB/Add)
        (.setCombineSrc0RGB    Texture$CombinerSource/CurrentTexture) ;;???
        (.setCombineOp0RGB     Texture$CombinerOperandRGB/SourceColor)
        (.setCombineSrc1RGB    Texture$CombinerSource/Previous)
        (.setCombineOp1RGB     Texture$CombinerOperandRGB/SourceColor)
        (.setCombineScaleRGB   Texture$CombinerScale/One)) 
      (doto textState
        (.setEnabled true)
        (.setTexture t1 0)
        (.setTexture t2 1))
      (. terrainBlock (setRenderState textState))
      (doto terrainBlock
        (.setRenderState textState)
        (.setDetailTexture 1 16)
        (.setRenderQueueMode 2))) ;; END ImageBasedHeightMap handling
    (let [beach   (Quad. "beach" 10240 10240)]
      (flipQuad beach)
      (setTexture beach ($get :display) :sand)
      (.setLocalTranslation beach (Vector3f. 0 2 0))
      (.updateModelBound beach)
      (.updateRenderState beach)
      (let [ newNode (Node. "Terrain block") ];(.. gamestate (getPhysicsSpace) (createStaticNode)) ]
;        (.attachChild   newNode beach)
        (.setLocalScale newNode (float 20))
        (.attachChild   newNode terrainBlock)
                                        ;   (.generatePhysicsGeometry true)
        newNode))))
      
;======= SKYBOX: START

(defn makeSkybox
  []
  (let [ skyBox   (Skybox. "Skybox" (float 700) (float 700) (float 700))
         north    (TextureManager/loadTexture (>Image :north) BilNoMipMap Bil true) 
         south    (TextureManager/loadTexture (>Image :south) BilNoMipMap Bil true) 
         east     (TextureManager/loadTexture (>Image :east)  BilNoMipMap Bil true) 
         west     (TextureManager/loadTexture (>Image :west)  BilNoMipMap Bil true) 
         top      (TextureManager/loadTexture (>Image :top)   BilNoMipMap Bil true) ]
    (doto skyBox
      (.setTexture Skybox$Face/North north)
      (.setTexture Skybox$Face/South south)
      (.setTexture Skybox$Face/East  east)
      (.setTexture Skybox$Face/West  west)
      (.setTexture com.jme.scene.Skybox$Face/Up    top)
      (.setTexture com.jme.scene.Skybox$Face/Down  top))
    skyBox))

;======= SKYBOX: STOP - WATER START

(defn makeAlphaForTransparency
  [display]
  (let [ as1   (.. display (getRenderer) (createBlendState)) ]
    (doto as1
      (.setBlendEnabled true)
      (.setSourceFunction  BlendState$SourceFunction/SourceAlpha)
      (.setDestinationFunction BlendState$DestinationFunction/OneMinusSourceAlpha)
      (.setTestEnabled true)
      (.setTestFunction BlendState$TestFunction/GreaterThan)
      (.setEnabled true)
      (.setBlendEnabled true))
    as1))


(defn initWaterQuad
  [quad height alpha texture display]
  (doto quad
    (.setRenderState     (makeAlphaForTransparency display))
    (.setRenderState     texture)
    (.setSolidColor      (ColorRGBA. 1 1 1 alpha))
    (.setRenderQueueMode com.jme.renderer.Renderer/QUEUE_TRANSPARENT)
    (.setLightCombineMode Spatial$LightCombineMode/Off)
    (.setModelBound (BoundingBox.))
    (.setLocalTranslation (Vector3f. 0 height 0))
    .updateModelBound
    .updateRenderState))

(defn buildWater
  [display]
  (let [ waterQuad1   (Quad. "water quad" 10240 10240)
         waterQuad2   (Quad. "water quad" 10240 10240)
         textureState (.. display (getRenderer) (createTextureState))
         t1           (TextureManager/loadTexture (>Image :water1) BilNoMipMap Bil true)
         t2           (TextureManager/loadTexture (>Image :water2) BilNoMipMap Bil true) ]
    (flipQuad waterQuad1)
    (flipQuad waterQuad2)
    (.setWrap t1 Texture$WrapMode/MirroredRepeat)
    (.setScale t1 (Vector3f. 100 100 1))
    (.setWrap t2 Texture$WrapMode/MirroredRepeat)
    (.setScale t2 (Vector3f. 100 100 1))
    (.setTexture textureState t1 0)
    (.setTexture textureState t2 0)
    (initWaterQuad waterQuad1 10   0.5 textureState display)
    (initWaterQuad waterQuad2 10.1 0.5 textureState display)
    (.setTranslation t1 (Vector3f. 0 0 0))
    (.setTranslation t2 (Vector3f. 0 0 0))
    {:quad1 waterQuad1 :quad2 waterQuad2 :tex1 t1 :tex2 t2}))
      
    
    
        