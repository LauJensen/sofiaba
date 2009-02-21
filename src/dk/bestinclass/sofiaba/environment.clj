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

;======= TERRAIN: START

(defn buildTerrain
  []
  (let [ heightMap     (MidPointHeightMap. 64 (float 1.0)) 
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
    (doto textState
      (.setEnabled true)
      (.setTexture
       (TextureManager/loadTexture
        (.getImage (.getImageIcon textGenerator)) BilNoMipMap Bil true)))
    (. terrainBlock (setRenderState textState))
    terrainBlock))
      
;======= SKYBOX: START

(defn makeSkybox
  []
  (let [ skyBox   (Skybox. "Skybox" 10 10 10)
         north    (TextureManager/loadTexture (.getImage (ImageIcon. (get-resource :north)))
                                              BilNoMipMap Bil true) 
         south    (TextureManager/loadTexture (.getImage (ImageIcon. (get-resource :south)))
                                              BilNoMipMap Bil true) 
         east     (TextureManager/loadTexture (.getImage (ImageIcon. (get-resource :east)))
                                              BilNoMipMap Bil true) 
         west     (TextureManager/loadTexture (.getImage (ImageIcon. (get-resource :west)))
                                              BilNoMipMap Bil true) 
         top      (TextureManager/loadTexture (.getImage (ImageIcon. (get-resource :top)))
                                              BilNoMipMap Bil true) ]
    (doto skyBox
      (.setTexture com.jme.scene.Skybox$Face/North north)
      (.setTexture com.jme.scene.Skybox$Face/South south)
      (.setTexture com.jme.scene.Skybox$Face/East  east)
      (.setTexture com.jme.scene.Skybox$Face/West  west)
      (.setTexture com.jme.scene.Skybox$Face/Up    top)
      (.setTexture com.jme.scene.Skybox$Face/Down  top))
    skyBox))
                                              