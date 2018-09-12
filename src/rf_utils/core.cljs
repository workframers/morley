(ns rf-utils.core
  "Helpers to cut-down on re-frame boilerplate, while also building up a
  performant map-centric signal graph. Most of this from https://goo.gl/Ydqbc8."
  (:refer-clojure :exclude [get get-in assoc assoc-in update update-in dissoc])
  (:require [clojure.core :as clj]
            [akiroz.re-frame.storage :as storage]
            [re-frame.core :as rf]
            [re-frame.db :as rfdb]))

(defn collify [x]
  (if (map? x)
    [x]
    (if (coll? x) x [x])))

(defn watch-sub
  "Given a subscription, call on-change-fn when it changes, passing the
  old-value and the new-value. Returns an unwatch function for cleanup. This is
  basically a convenience function around add-watch/remove-watch on re-frame
  subscriptions."
  [sub on-change-fn]
  (let [kw (keyword (gensym "SUB"))]
    (add-watch sub kw (fn [_ _ old-val new-val] (on-change-fn old-val new-val)))
    #(remove-watch sub kw)))

(defn local-storage
  "A re-frame interceptor that, after an event, will persist to local storage
  anything in the app-db that exists under the `:local-storage` key. If the
  `:sync-before` flag is passed, the interceptor will read from local storage
  and reset the value at the `:local-storage` key to what was read before
  running the event handler."
  [& [flag]]
  (let [store-key :wf-app-db-persistent
        db-ls-key :local-storage]
    (storage/register-store store-key)
    (rf/->interceptor
      :id     :local-storage
      :before (fn [context]
                (if (= flag :sync-before)
                  (clj/assoc-in context [:coeffects :db db-ls-key] (storage/<-store store-key))
                  context))
      :after  (fn [context]
                (when-let [value (clj/get-in context [:effects :db db-ls-key])]
                  (storage/->store store-key value))
                context))))

(defn listen
  "Listen to a re-frame subscription, so we can more easily use it when writing
  Reagent components. Probably don't want to use this inside of a lazy `seq`
  (for, map, etc.). cf: https://goo.gl/MWsDz5"
  [qv]
  @(rf/subscribe (collify qv)))

(rf/reg-sub
  ::app-db
  (fn [db _]
    db))

(rf/reg-sub
  ::get
  (fn [db [_ k not-found]]
    (clj/get db k not-found)))

(defn get
  "Read a value from db by `k`, `not-found` or `nil` if value not present."
  ([k]
   (listen [::get k]))
  ([k not-found]
   (listen [::get k not-found])))

(rf/reg-sub
  ::get-in
  (fn [[_ path & [not-found]]]
    (if (<= (count path) 1)
      rfdb/app-db
      (rf/subscribe [::get-in (drop-last path) not-found])))
  (fn [parent [_ path & [not-found]]]
    (if (empty? path)
      parent
      (clj/get parent (last path) not-found))))

(defn get-in
  "Read a value from db by `path`, `not-found` or `nil` if value not present."
  ([path]
   (listen [::get-in path]))
  ([path not-found]
   (listen [::get-in path not-found])))

(defn- dispatch-into [k args & [sync?]]
  (let [disp-fn (if sync? rf/dispatch-sync rf/dispatch)]
    (disp-fn (into [k] args))))

(rf/reg-event-db
  ::assoc
  (fn [db [_ & kvs]]
    (apply clj/assoc db kvs)))

(defn assoc
  "Applies assoc to app-db with `args`."
  [& args]
  (dispatch-into ::assoc args))

(defn assoc-sync
  "Synchronously applies assoc to app-db with `args`. Should only be used to
  init state."
  [& args]
  (dispatch-into ::assoc args true))

(rf/reg-event-db
  ::assoc-in
  (local-storage)
  (fn [db [_ path v]]
    (clj/assoc-in db path v)))

(defn assoc-in
  "Applies assoc-in to app-db with `args`."
  [& args]
  (dispatch-into ::assoc-in args))

(defn assoc-in-sync
  "Synchronously applies assoc-in to app-db with `args`. Should only be used
  to init state."
  [& args]
  (dispatch-into ::assoc-in args true))

(rf/reg-event-db
  ::update
  (fn [db [_ k f & args]]
    (apply clj/update db k f args)))

(defn update
  "Applies update to app-db with `args`."
  [& args]
  (dispatch-into ::update args))

(defn update-sync
  "Applies update to app-db with `args`."
  [& args]
  (dispatch-into ::update args true))

(rf/reg-event-db
  ::update-in
  (local-storage)
  (fn [db [_ path f & args]]
    (apply clj/update-in db path f args)))

(defn update-in
  "Applies update-in to app-db with `args`."
  [& args]
  (dispatch-into ::update-in args))

(defn update-in-sync
  "Applies update-in to app-db with `args`."
  [& args]
  (dispatch-into ::update-in args true))

(rf/reg-event-db
  ::dissoc
  (fn [db [_ & ks]]
    (apply clj/dissoc db ks)))

(defn dissoc
  "Applies dissoc to app-db with `args`."
  [& args]
  (dispatch-into ::dissoc args))

(rf/reg-event-db
  ::dissoc-in
  (local-storage)
  (fn [db [_ path]]
    (clj/update-in db (butlast path) clj/dissoc (last path))))

(defn dissoc-in
  "Applies dissoc-in to app-db with `args`."
  [& args]
  (dispatch-into ::dissoc-in args))

(rf/reg-event-db
  ::reset
  (fn [db _]
    (.warn js/console "Resetting store")
    (empty db)))

(defn reset
  "Clears the store. Useful for testing."
  []
  (dispatch-into ::reset nil))

(defn watch
  "Same thing as lunar.lib.util/watch-sub, but allows you to pass a get or get-in
  value instead of a full subscription, so that it feels more like all the
  other utils."
  [path on-change-fn]
  (let [p   (if (vector? path) path [path])
        sub (rf/subscribe [::get-in p])]
    (watch-sub sub on-change-fn)))
