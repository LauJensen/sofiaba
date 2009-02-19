(import '(com.jme.app SimpleGame AbstractGame$ConfigShowMode))
 
(doto (proxy [SimpleGame] []
        (simpleInitGame []
          (prn :init)))
  (.setConfigShowMode AbstractGame$ConfigShowMode/AlwaysShow)
  (.start))