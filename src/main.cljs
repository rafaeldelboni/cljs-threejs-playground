(ns main
  (:require [load-gltf :as load-gltf]
            [voxel-geometry :as voxel-geometry]))

(defn handle-click-by-id [id handler]
  (let [el (js/document.getElementById id)]
    (.addEventListener el "click" (fn [_event] (handler)))))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn init []
  (handle-click-by-id "li-load-gltf" load-gltf/init)
  (handle-click-by-id "li-voxel-geometry" voxel-geometry/init)
  ; default scene
  ;(load-gltf/init)
  (voxel-geometry/init))
