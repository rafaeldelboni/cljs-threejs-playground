(ns main
  (:require [load-gltf :as load-gltf]))

(defn handle-click-by-id [id handler]
  (let [el (js/document.getElementById id)]
    (.addEventListener el "click" (fn [_event] (handler)))))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn init []
  (handle-click-by-id "load-0" load-gltf/init)
  ; default scene
  (load-gltf/init))
