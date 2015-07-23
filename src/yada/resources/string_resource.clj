;; Copyright © 2015, JUXT LTD.

(ns yada.resources.string-resource
  (:require
   [clj-time.core :refer (now)]
   [clj-time.coerce :refer (to-date)]
   [yada.resource :refer [Resource ResourceRepresentations ResourceConstructor ResourceFetch platform-charsets]]
   [yada.methods :refer [Get Options]]
   [yada.representation :refer [Representation]]))

(defrecord StringResource [s last-modified]
  Resource
  ;; Don't include :head, it is always available with yada.
  (methods [this] #{:get :options})
  (parameters [_] nil)
  (exists? [this ctx] true)
  (last-modified [this _] last-modified)

  ResourceRepresentations
  (representations [_]
    [{;; Without attempting to actually parse it (which isn't completely
      ;; impossible) we're not able to guess the media-type of this
      ;; string, so we return text/plain.
      :content-type #{"text/plain"}
      :charset platform-charsets}])

  Get
  (get* [this ctx] s))

(extend-protocol ResourceConstructor
  String
  (make-resource [s]
    (->StringResource s (to-date (now)))))