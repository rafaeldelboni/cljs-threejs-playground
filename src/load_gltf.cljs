(ns load-gltf
  (:require [applied-science.js-interop :as j]
            [common :as common]
            ["three" :as three]
            ["three/examples/jsm/controls/OrbitControls" :as orbit-controls]
            ["three/examples/jsm/loaders/GLTFLoader" :as gltf-loader]))

(defn init-three-state []
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

(defn frame-area [size-to-fit-on-screen box-size box-center camera]
  (let [half-size-to-fit-on-screen (* size-to-fit-on-screen 0.5)
        half-fov-y (.degToRad three/MathUtils (* (. camera -fov) 0.5))
        distance (/ half-size-to-fit-on-screen (Math/tan half-fov-y))
        direction (doto (three/Vector3.)
                    (.subVectors (. camera -position) box-center)
                    (.multiply (three/Vector3. 1 0 1))
                    .normalize)]
    (doto ^three/PerspectiveCamera camera
      (j/call-in [:position :copy]
                 (-> direction
                     (.multiplyScalar distance)
                     (.add box-center)))
      (j/assoc! :near (/ box-size 100)
                :far (* box-size 100))
      .updateProjectionMatrix
      (.lookAt box-center))))

(defn load-gltf-model [{:keys [camera controls scene]}]
  (.load (gltf-loader/GLTFLoader.)
         "https://threejs.org/manual/examples/resources/models/cartoon_lowpoly_small_city_free_pack/scene.gltf"
         (fn [gltf]
           (let [root (. gltf -scene)
                 box (.setFromObject (three/Box3.) root)
                 box-size (-> box (.getSize (three/Vector3.)) .length)
                 box-center (.getCenter box (three/Vector3.))]
             (.add scene root)
             (frame-area (* box-size 0.5) box-size box-center camera)
             (doto controls
               (j/assoc! :maxDistance (* box-size 10))
               (j/call-in [:target :copy] box-center)
               .update)))))

(defn init []
  (let [{:keys [scene] :as three-data} (init-three-state)]
    (-> scene
        ->add-plane-mesh
        ->add-hemisphere-light
        (common/->add-directional-light 5 10 2))
    (load-gltf-model three-data)
    (common/resize-canvas three-data)
    (common/render-fn three-data)
    (clj->js three-data)))
