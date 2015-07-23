;; Copyright © 2015, JUXT LTD.

(ns yada.resources.atom-resource
  (:refer-clojure :exclude [methods])
  (:require
   [clj-time.core :refer [now]]
   [clj-time.coerce :refer [to-date]]
   [yada.resource :refer (ResourceConstructor ResourceRepresentations Resource platform-charsets make-resource parameters methods)]
   [yada.methods :refer (Get Put)]
   [schema.core :as s]
   yada.resources.string-resource)
  (:import [yada.resources.string_resource StringResource]))

(defprotocol StateWrapper
  (wrap-atom [init-state a] "Given the initial value on derefencing an atom, construct a record which will manage the reference."))

(defrecord AtomicMapResource [*a wrapper *last-mod]
  Resource
  (methods [_] (conj (set (methods wrapper)) :put :post :delete))
  (parameters [_] (assoc (parameters wrapper) :put {:body s/Str})) ;; TODO: Not just a string, depends on wrapper

  (exists? [_ ctx] true)

  (last-modified [_ ctx]
    (when-let [lm @*last-mod]
      lm))

  ResourceRepresentations
  (representations [_] [{:method #{:get :head}
                         :content-type #{"application/edn" "text/html;q=0.9" "application/json;q=0.9"}
                         :charset platform-charsets}])

  Get
  (get* [_ ctx] @*a)

  Put
  (put [_ ctx] (reset! *a (get-in ctx [:parameters :body]))))

(defn wrap-with-watch [wrapper *a]
  (let [*last-mod (atom nil)]
    (-> *a
        ;; We add a watch to the atom so we can record when it gets
        ;; modified.
        (add-watch :last-modified
                   (fn [_ _ _ _]
                     (reset! *last-mod (to-date (now)))))
        (->AtomicMapResource wrapper *last-mod))))

(extend-protocol StateWrapper
  clojure.lang.APersistentMap
  (wrap-atom [this *a] (wrap-with-watch this *a))
  StringResource
  (wrap-atom [this *a] (wrap-with-watch this *a)))

(extend-protocol ResourceConstructor
  clojure.lang.Atom
  (make-resource [a]
    (wrap-atom (make-resource @a) a)))