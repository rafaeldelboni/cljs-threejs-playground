(ns voxel-geometry
  (:require [applied-science.js-interop :as j]
            [common :as common]
            ["three" :as three]
            ["three/examples/jsm/controls/OrbitControls" :as orbit-controls]))

(def voxel-faces
  [{:uv-row 0
    :dir [-1 0 0]
    :corners [{:pos [0 1 0] :uv [0 1]}
              {:pos [0 0 0] :uv [0 0]}
              {:pos [0 1 1] :uv [1 1]}
              {:pos [0 0 1] :uv [1 0]}]}
   {:uv-row 0
    :dir [1 0 0]
    :corners [{:pos [1 1 1] :uv [0 1]}
              {:pos [1 0 1] :uv [0 0]}
              {:pos [1 1 0] :uv [1 1]}
              {:pos [1 0 0] :uv [1 0]}]}
   {:uv-row 1
    :dir [0 -1 0]
    :corners [{:pos [1 0 1] :uv [1 0]}
              {:pos [0 0 1] :uv [0 0]}
              {:pos [1 0 0] :uv [1 1]}
              {:pos [0 0 0] :uv [0 1]}]}
   {:uv-row 2
    :dir [0 1 0]
    :corners [{:pos [0 1 1] :uv [1 1]}
              {:pos [1 1 1] :uv [0 1]}
              {:pos [0 1 0] :uv [1 0]}
              {:pos [1 1 0] :uv [0 0]}]}
   {:uv-row 0
    :dir [0 0 -1]
    :corners [{:pos [1 0 0] :uv [0 0]}
              {:pos [0 0 0] :uv [1 0]}
              {:pos [1 1 0] :uv [0 1]}
              {:pos [0 1 0] :uv [1 1]}]}
   {:uv-row 0
    :dir [0 0 1]
    :corners [{:pos [0 0 1] :uv [0 0]}
              {:pos [1 0 1] :uv [1 0]}
              {:pos [0 1 1] :uv [0 1]}
              {:pos [1 1 1] :uv [1 1]}]}])

(defn new-voxel-world [cell-size]
  {:cell-size cell-size
   :cell-slice-size (* cell-size cell-size)
   :tile-size 16
   :tile-texture-width 256
   :tile-texture-height 64
   :cells {}
   :faces voxel-faces})

(defn load-texture []
  (doto (.load (three/TextureLoader.)
               "https://threejs.org/manual/examples/resources/images/minecraft/flourish-cc-by-nc-sa.png")
    (-> (j/assoc!
         :magFilter three/NearestFilter
         :minFilter three/NearestFilter))))

(defn init-three-state [cell-size]
  (let [{:keys [fov aspect near far]} {:fov 75 :aspect 2 :near 0.1 :far 1000}
        canvas (-> js/document (.querySelector "#three-canvas"))
        renderer (three/WebGLRenderer. (clj->js {:canvas canvas}))
        camera (doto (three/PerspectiveCamera. fov aspect near far)
                 (j/apply-in [:position :set] #js[(* cell-size -0.3)
                                                  (* cell-size 0.8)
                                                  (* cell-size -0.3)]))
        controls (doto (orbit-controls/OrbitControls. camera canvas)
                   (j/apply-in [:target :set] #js[(/ cell-size 2)
                                                  (/ cell-size 3)
                                                  (/ cell-size 2)])
                   (.update))
        scene (three/Scene.)]
    (set! (. scene -background) (three/Color. "lightblue"))
    {:world (new-voxel-world cell-size)
     :renderer renderer
     :camera camera
     :controls controls
     :scene scene}))

(defn compute-voxel-offset
  [{:keys [cell-size cell-slice-size]} x y z]
  (let [voxel-x (bit-or (three/MathUtils.euclideanModulo x cell-size) 0)
        voxel-y (bit-or (three/MathUtils.euclideanModulo y cell-size) 0)
        voxel-z (bit-or (three/MathUtils.euclideanModulo z cell-size) 0)]
    (+ (+ (* voxel-y cell-slice-size)
          (* voxel-z cell-size))
       voxel-x)))

(defn valid-voxel?
  [{:keys [cell-size]} x y z]
  (let [cell-x (Math/floor (/ x cell-size))
        cell-y (Math/floor (/ y cell-size))
        cell-z (Math/floor (/ z cell-size))]
    (and (= cell-x 0)
         (= cell-y 0)
         (= cell-z 0))))

(defn set-voxel
  [world x y z value]
  (assoc-in world [:cells (compute-voxel-offset world x y z)] value))

(defn get-voxel
  [{:keys [cells] :as world} x y z]
  (let [default-value 0]
    (if (valid-voxel? world x y z)
      (get cells (compute-voxel-offset world x y z))
      default-value)))

(defn rand-int-min-max
  [min max]
  (+ min (rand-int max)))

(defn generate-random-cells
  [{:keys [cell-size] :as voxel-world}]
  (->> (for [y (range cell-size)
             z (range cell-size)
             x (range cell-size)
             :let [height (+ (* (+ (Math/sin (* (* (/ x cell-size) Math/PI) 2))
                                   (Math/sin (* (* (/ z cell-size) Math/PI) 3)))
                                (/ cell-size 6))
                             (/ cell-size 2))]
             :when (< y height)]
         [x y z])
       (reduce (fn [acc [x y z]] (set-voxel acc x y z (rand-int-min-max 1 17)))
               voxel-world)))

(defn eager-flatten [l]
  (loop [l1 l, l2 `()]
    (cond
      (sequential? (first l1)) (recur (concat (first l1) (rest l1)) l2)
      (empty? l1) (reverse l2)
      :else (recur (rest l1) (cons (first l1) l2)))))

; TODO: slow
(defn calculate-geometry-data-for-cell
  [{:keys [cell-size faces tile-size tile-texture-width tile-texture-height] :as world}
   cell-x cell-y cell-z]
  (let [start-x (* cell-x cell-size)
        start-y (* cell-y cell-size)
        start-z (* cell-z cell-size)]
    (eager-flatten
     (for [y (range cell-size)
           z (range cell-size)
           x (range cell-size)
           :let [voxel-y (+ start-y y)
                 voxel-z (+ start-z z)
                 voxel-x (+ start-x x)
                 voxel (get-voxel world voxel-x voxel-y voxel-z)]
           :when (> voxel 0)]
       (->> faces
            (map (fn [{:keys [dir corners uv-row]}]
                   {:normals (repeat 4 dir)
                    :corners (map (fn [{:keys [uv pos]}]
                                    {:position [(+ x (get pos 0))
                                                (+ y (get pos 1))
                                                (+ z (get pos 2))]
                                     :uv [(/ (* (+ (- voxel 1) (get uv 0)) tile-size)
                                             tile-texture-width)
                                          (- 1 (/ (* (- (+ uv-row 1) (get uv 1)) tile-size)
                                                  tile-texture-height))]})
                                  corners)
                    :neighbor (get-voxel world
                                         (+ voxel-x (get dir 0))
                                         (+ voxel-y (get dir 1))
                                         (+ voxel-z (get dir 2)))}))
            (filter (fn [{:keys [neighbor]}]
                      (or (nil? neighbor)
                          (zero? neighbor)))))))))

; TODO: slow
(defn world->geometry-attributes
  [world]
  (let [normals (->> world (map :normals) eager-flatten)
        positions (->> world (map :corners) eager-flatten (map :position) eager-flatten)
        uvs (->> world (map :corners) eager-flatten (map :uv) eager-flatten)
        num-indices (-> positions count (/ 3))
        indices (->> (range 0 num-indices 4)
                     (map (fn [ndx]
                            [ndx
                             (+ ndx 1)
                             (+ ndx 2)
                             (+ ndx 2)
                             (+ ndx 1)
                             (+ ndx 3)]))
                     eager-flatten)]
    {:normals normals
     :positions positions
     :uvs uvs
     :indices indices}))

(defn ->add-geometry-mesh [scene {:keys [positions normals uvs indices]}]
  (let [position-num-components 3
        normal-num-components 3
        uv-num-components 2
        texture (load-texture)
        geometry (doto (three/BufferGeometry.)
                   (.setAttribute
                    "position"
                    (three/BufferAttribute. (js/Float32Array. positions) position-num-components))
                   (.setAttribute
                    "normal"
                    (three/BufferAttribute. (js/Float32Array. normals) normal-num-components))
                   (.setAttribute
                    "uv"
                    (three/BufferAttribute. (js/Float32Array. uvs) uv-num-components))
                   (.setIndex (clj->js indices)))
        material (three/MeshLambertMaterial. (clj->js {:map texture
                                                       :side three/DoubleSide
                                                       :alphaTest 0.1
                                                       :transparent true}))
        mesh (three/Mesh. geometry material)]
    (.add scene mesh)))

(defn init []
  (let [cell-size 32
        {:keys [scene] :as three-data} (init-three-state cell-size)
        geometry-attributes (-> cell-size
                                new-voxel-world
                                generate-random-cells
                                (calculate-geometry-data-for-cell 0 0 0)
                                world->geometry-attributes)]
    (-> scene
        (common/->add-directional-light -1 2 4)
        (common/->add-directional-light 1 -1 -2)
        (->add-geometry-mesh geometry-attributes))
    (common/resize-canvas three-data)
    (common/render-fn three-data)
    (clj->js three-data)))
