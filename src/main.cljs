(ns main
  (:require [applied-science.js-interop :as j]
            ["three" :as three]
            ["three/examples/jsm/controls/OrbitControls" :as orbit-controls]
            ["three/examples/jsm/loaders/GLTFLoader" :as gltf-loader]))

(defn init-three []
  (let [{:keys [fov aspect near far]} {:fov 45 :aspect 2 :near 0.1 :far 100}
        canvas (-> js/document (.querySelector "#three-canvas"))
        renderer (three/WebGLRenderer. (clj->js {:canvas canvas}))
        camera (doto (three/PerspectiveCamera. fov aspect near far)
                 (j/apply-in [:position :set] #js[0 10 20]))
        controls (doto (orbit-controls/OrbitControls. camera canvas)
                   (j/apply-in [:target :set] #js[0 5 0])
                   (.update))
        scene (three/Scene.)]
    (set! (. scene -background) (three/Color. "cyan"))
    {:renderer renderer
     :camera camera
     :controls controls
     :scene scene}))

(defn resize-canvas [camera renderer]
  (let [canvas (. renderer -domElement)
        client-width (. canvas -clientWidth)
        client-height (. canvas -clientHeight)
        width (. canvas -width)
        height (. canvas -height)
        camera-projection (/ client-width client-height)
        need-resize (or (not= client-width width) (not= client-height height))]
    (when need-resize
      (.setSize renderer client-width client-height false)
      (doto ^js/undefined camera
        (j/assoc! :aspect camera-projection)
        .updateProjectionMatrix))))

(defn render-fn [{:keys [camera renderer scene] :as three-data}]
  (.requestAnimationFrame js/window #(render-fn three-data))
  (resize-canvas camera renderer)
  (.render renderer scene camera))

(defn ->add-plane-mesh [scene]
  (let [plane-size 40
        repeats (/ plane-size 2)
        texture (doto (.load (three/TextureLoader.)
                             "https://threejs.org/manual/examples/resources/images/checker.png")

                  (-> (j/assoc!
                       :wrapS three/RepeatWrapping
                       :wrapT three/RepeatWrapping
                       :magFilter three/NearestFilter)
                      (j/apply-in [:repeat :set] #js[repeats repeats])))
        plane-geo (three/PlaneGeometry. plane-size plane-size)
        plane-mat (three/MeshPhongMaterial. (clj->js {:map texture
                                                      :side three/DoubleSide}))
        mesh (doto (three/Mesh. plane-geo plane-mat)
               (j/assoc-in! [:rotation :x] (* Math/PI -0.5)))]
    (.add scene mesh)))

(defn ->add-hemisphere-light [scene]
  (let [sky-color 0xB1E1FF
        ground-color 0xB97A20
        intensity 1
        light (three/HemisphereLight. sky-color ground-color intensity)]
    (.add scene light)))

(defn ->add-directional-light [scene]
  (let [color 0xFFFFFF
        intensity 1
        light (doto (three/DirectionalLight. color intensity)
                (j/apply-in [:position :set] #js[5 10 2]))]
    (.add scene light)
    (.add scene (j/get light :target))))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn init []
  (let [{:keys [scene] :as three-data} (init-three)]
    (-> scene
        ->add-plane-mesh
        ->add-hemisphere-light
        ->add-directional-light)
    (render-fn three-data))
  (js/console.log "hello from cljs!"))
