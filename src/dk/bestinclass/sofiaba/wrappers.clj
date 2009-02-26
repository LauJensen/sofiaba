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


(def *resources*
     {:logo       "res/logo.png"

      :water      "res/textures/water.jpg"
      :water1     "res/textures/water1.jpg"
      :water2     "res/textures/water2.jpg"
      :sand       "res/textures/sand.jpg"
      :grass      "res/textures/grass.jpg"
      :dirt       "res/textures/dirt.jpg"
      :highest    "res/textures/highest.jpg"
      
      :north      "res/skybox/north.jpg"
      :south      "res/skybox/south.jpg"
      :west       "res/skybox/west.jpg"
      :east       "res/skybox/east.jpg"
      :top        "res/skybox/top.jpg"

      :detailmap  "res/textures/detail.jpg"
      :heightmap  "res/textures/heightmap.jpg"
      :nunna      "res/textures/objects/nunna.jpg"} )
     

(defn get-resource-uri
  [res]
  (if-let [ uri  (. (.. Class (forName "dk.bestinclass.sofiaba") (getClassLoader))
                    (findResource (str "dk/bestinclass/" res)))]
          uri
          (throw (Exception. (format "Resource not found: %s" res)))))

(defn get-resource
  [key]
  (get-resource-uri (*resources* key)))

;======= RESOURCES: STOP - HELPERS START



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
  [obj display tex]
  (let [texState  (.. display (getRenderer) (createTextureState))
        texture   (TextureManager/loadTexture (>Image tex) BilNoMipMap Bil true) ]
    (.setTexture texState texture 0)
    (.setRenderState obj texState)))

