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

(def BilNoMipMap com.jme.image.Texture$MinificationFilter/BilinearNoMipMaps)
(def Bil         com.jme.image.Texture$MagnificationFilter/Bilinear)

(defn >Image
  " A helper to convert a file to an Image, via ImageIcon "
  [resource-key]
  (if (keyword? resource-key)
    (.getImage (ImageIcon. (get-resource resource-key)))
    (.getImage (.getImageIcon resource-key))))

;======= TERRAIN: START

(defn buildTerrain
  []
  (let [ heightMap     (MidPointHeightMap. 512 (float 0.80)) 
         terrainScale  (Vector3f. 20 (float 0.5) 20) 
         terrainBlock  (TerrainBlock. "Terrain" (.getSize heightMap) terrainScale
                                   (.getHeightMap heightMap) (Vector3f. 0 0 0)) 
         textGenerator (ProceduralTextureGenerator. heightMap) 
         textState     (.. ($get :display) (getRenderer) (createTextureState)) ]
    (doto terrainBlock
      (.setModelBound (BoundingBox.))
      .updateModelBound)
    (doto textGenerator
      (.addTexture (ImageIcon. (get-resource :grass)) -128   0 128)
      (.addTexture (ImageIcon. (get-resource :dirt))     0 128 255)
      (.addTexture (ImageIcon. (get-resource :dirt))   128 255 384)
      (.createTexture 32))
    (let
        [t1 (TextureManager/loadTexture (>Image textGenerator) BilNoMipMap Bil true)
         t2 (TextureManager/loadTexture (>Image :detailmap)    BilNoMipMap Bil true) ]
      (. t2 (setWrap  com.jme.image.Texture$WrapMode/MirroredRepeat))
      (doto t1
        (.setApply             com.jme.image.Texture$ApplyMode/Combine)
        (.setCombineFuncRGB    com.jme.image.Texture$CombinerFunctionRGB/Modulate)
        (.setCombineSrc0RGB    com.jme.image.Texture$CombinerSource/CurrentTexture) ;;???
        (.setCombineOp0RGB     com.jme.image.Texture$CombinerOperandRGB/SourceColor)
        (.setCombineSrc1RGB    com.jme.image.Texture$CombinerSource/PrimaryColor)
        (.setCombineOp1RGB     com.jme.image.Texture$CombinerOperandRGB/SourceColor)
        (.setCombineScaleRGB   com.jme.image.Texture$CombinerScale/One))
      (doto t2
        (.setApply             com.jme.image.Texture$ApplyMode/Combine)
        (.setCombineFuncRGB    com.jme.image.Texture$CombinerFunctionRGB/Add)
        (.setCombineSrc0RGB    com.jme.image.Texture$CombinerSource/CurrentTexture) ;;???
        (.setCombineOp0RGB     com.jme.image.Texture$CombinerOperandRGB/SourceColor)
        (.setCombineSrc1RGB    com.jme.image.Texture$CombinerSource/Previous)
        (.setCombineOp1RGB     com.jme.image.Texture$CombinerOperandRGB/SourceColor)
        (.setCombineScaleRGB   com.jme.image.Texture$CombinerScale/One))
      (doto textState
        (.setEnabled true)
        (.setTexture t1 0)
        (.setTexture t2 1))
      (. terrainBlock (setRenderState textState))
      (doto terrainBlock
        (.setRenderState textState)
        (.setDetailTexture 1 16)
        (.setRenderQueueMode 2)))
    terrainBlock))
      
;======= SKYBOX: START

(defn makeSkybox []
  (let [ skyBox   (Skybox. "Skybox" 10 10 10)
         north    (TextureManager/loadTexture (>Image :north) BilNoMipMap Bil true) 
         south    (TextureManager/loadTexture (>Image :south) BilNoMipMap Bil true) 
         east     (TextureManager/loadTexture (>Image :east)  BilNoMipMap Bil true) 
         west     (TextureManager/loadTexture (>Image :west)  BilNoMipMap Bil true) 
         top      (TextureManager/loadTexture (>Image :top)   BilNoMipMap Bil true) ]
    (doto skyBox
      (.setTexture com.jme.scene.Skybox$Face/North north)
      (.setTexture com.jme.scene.Skybox$Face/South south)
      (.setTexture com.jme.scene.Skybox$Face/East  east)
      (.setTexture com.jme.scene.Skybox$Face/West  west))
;      (.setTexture com.jme.scene.Skybox$Face/Up    top)
 ;     (.setTexture com.jme.scene.Skybox$Face/Down  top))
    skyBox))
                                              