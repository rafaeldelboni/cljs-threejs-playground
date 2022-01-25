(ns common
  (:require [applied-science.js-interop :as j]
            ["three" :as three]))

(defn resize-canvas [{:keys [camera renderer]}]
  (let [canvas (. renderer -domElement)
        client-width (. canvas -clientWidth)
        client-height (. canvas -clientHeight)
        camera-projection (/ client-width client-height)]
    (.setSize renderer client-width client-height false)
    (doto ^three/PerspectiveCamera camera
      (j/assoc! :aspect camera-projection)
      .updateProjectionMatrix)))

(defn resize-canvas-when-needed [{:keys [renderer] :as three-data}]
  (let [canvas (. renderer -domElement)
        client-width (. canvas -clientWidth)
        client-height (. canvas -clientHeight)
        width (. canvas -width)
        height (. canvas -height)
        need-resize (or (not= client-width width) (not= client-height height))]
    (when need-resize
      (resize-canvas three-data))))

(defn render-fn [{:keys [camera renderer scene] :as three-data}]
  (.requestAnimationFrame js/window #(render-fn three-data))
  (resize-canvas-when-needed three-data)
  (.render renderer scene camera))

(defn ->add-directional-light [scene x y z]
  (let [color 0xFFFFFF
        intensity 1
        light (doto (three/DirectionalLight. color intensity)
                (j/apply-in [:position :set] #js[x y z]))]
    (.add scene light)))
