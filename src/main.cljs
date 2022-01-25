(ns main
  (:require [load-gltf :as load-gltf]
            [voxel-geometry :as voxel-geometry]))

(defonce state (atom #js {}))

(defn handle-click-by-id [id handler]
  (let [el (js/document.getElementById id)]
    (.addEventListener el "click" (fn [_event] (handler)))))

(defn unload! []
  (let [{:keys [controls renderer]} @state]
    (when controls (.dispose controls))
    (when renderer (.dispose renderer))
    (js-delete @state "camera")
    (js-delete @state "scene")
    (js-delete @state "controls")
    (js-delete @state "renderer")
    (reset! state {})))

(defn load!
  [three-data]
  (reset! state three-data))

(defn start [init-fn]
  (unload!)
  (load! (init-fn)))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn init []
  (handle-click-by-id "li-load-gltf" #(start load-gltf/init))
  (handle-click-by-id "li-voxel-geometry" #(start voxel-geometry/init))
  ; default scene
  (start voxel-geometry/init))
