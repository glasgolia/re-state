(ns glasgolia.re-state
  (:require
    [reagent.core :as ra]
    [reagent.ratom :as ratom]
    [glasgolia.common :refer [dissoc-in]]
    [glasgolia.glas-state.core :as gs]))



(defn create-service
  "Create a new statechart service.
  Expects a statechart-machine definition map and an optional config map.
  The state of the service is going to be stored in a reagent atom, making it
  easy to react to state changes."
  ([machine config]
   (let [state-atom (or (:state-atom config) (ra/atom {}))
         value (ra/cursor state-atom [:value])
         context (ra/cursor state-atom [:context])
         value-list (ratom/make-reaction (fn [] (gs/value-to-ids @value)))
         user-data (:service-data config)
         service-data {:value      value
                       :context    context
                       :value-list value-list
                       :user-data user-data}
         new-config (merge config {:state-atom state-atom :service-data service-data :change-listener (gs/service-logger)})
         service (gs/create-service machine new-config)]
     service))
  ([machine]
   (create-service machine {})))
(defn state
  "Returns the state reagent atom for the service"
  [service] (:storage service))

(def start
  "Starts a statechart service.
  See glas-state/start"
  gs/start)
(def stop
  "Stops a statechart service.
  See glas-state/stop"
  gs/stop)

(def dispatch
  "Dispatch an event to a service.
  See glas/state/dispatch"
  gs/dispatch)

(defn machine [service]
  "Get the machine out of a service"
  (:machine service))
(defn service-id [service]
  "Get the service-id of a service."
  (:id (:machine service)))
(defn service-data [service]
  (get-in service [:service-data :user-data]))

(defn value [{:keys [service-data] :as _service}]
  "Get the current statechart value as a reagent atom from the service.
  Expects a service."
  (:value service-data))

(defn context [{:keys [service-data] :as _service}]
  "Get the context reagent atom for a  service.
  Expects a service."
  (:context service-data))

(defn context-value [service path]
  "Get a specific value, as a reagent atom, out of a service context.
  Expects a service and a path (like you would use in the get-in function."
  (ratom/make-reaction #(get-in @(context service) path)))

(defn value-list [{:keys [service-data] :as _service}]
  "Get a reagent atom with a vector of all active state ids in a service.
  See glas-state/value-to-ids.
  Expects a service."
  (:value-list service-data))

(defn with-local-machine [machine render-fn]
  "Creates a reagent component using a statechart machine and a component render function.
  Expects a statechart machine and a component render function that takes a service as argument.
  Advantages of this is that the services is automatically started and stopped when the component
  is used or unmounted."
  (let [service (create-service machine)]
    (ra/create-class
      {:display-name           (str (service-id service))
       :reagent-render         (fn [_comp]
                                 (render-fn service))
       :component-will-unmount (fn [_comp]
                                 (stop service))})))
(defn with-local-service [service render-fn]
  "Creates a reagent component using a service and a component render function.
  Expects a service and a component render function that takes a service as argument.
  The service will be stopped when the component unmount."
  (ra/create-class
    {:display-name           (str (service-id service))
     :reagent-render         (fn [_comp]
                               (render-fn service))
     :component-will-unmount (fn [_comp]
                               (stop service))}))
(defn value? [service state-id]
  "Returns a reagent atom containing a boolean that indicates if a state is active.
  Expects a service and a state id"
  (let [vl (value-list service)
        id (name state-id)]
    (ratom/make-reaction #(contains? @vl id))))

(defn map-value [service state-id true-value false-value]
  "Get a reagent atom with that contains different values depending on if a given state is active.
  Expects a service, state id and the active-value and false-value"
  (let [vl (value-list service)
        id (name state-id)]
    (ratom/make-reaction
      #(if (contains? @vl id)
        true-value
        false-value))))

(def assign
  "See glas-state/assign"
  gs/assign)
;(def warn (.-warn js/console))
;
;(def sc-service-db (atom {}))
;(defn remove-service [service-id]
;  (swap! sc-service-db
;         (fn [s]
;           (let [service (get-in s service-id)]
;             (if service
;               (do
;                 (gs/stop service)
;                 (dissoc-in s service-id))
;               s)
;             )))
;  (swap! sc-state-db dissoc-in service-id))
;
;(defn send-service [service-id event]
;  (let [service (get-in @sc-service-db service-id)]
;    (gs/dispatch service event)))
;
;(defn create-service-callback [service-id]
;  (fn [event]
;    (send-service service-id event)))
;(defn create-service
;  ([machine {:keys [id parent-service-id] :as config}]
;   (let [this-id (or id [(:id machine)])
;         parent-service (when parent-service-id (get-in @sc-service-db parent-service-id))
;         new-service-id (into [] (concat parent-service-id this-id))
;         state-atom (ra/cursor sc-state-db new-service-id)
;         this-config (merge {:change-listener gs/service-logger
;                             :state-atom      state-atom}
;                            (when parent-service {:parent-callback (create-service-callback parent-service-id)}))
;         full-config (merge this-config config)
;         _ (println "----------------> creating service "  new-service-id full-config)
;         new-service (-> (gs/create-service machine full-config)
;                         (gs/start))]
;     (when (get-in @sc-service-db new-service-id)
;       (warn "Replacing service with new version for " new-service-id)
;       (remove-service new-service-id))
;     (swap! sc-service-db #(assoc-in % new-service-id new-service))
;     new-service-id))
;  ([machine]
;   (create-service machine {})))
;
;(def next-temp-id
;  (let [next-id (atom 0)]
;    (fn []
;      (swap! next-id inc))))
;
;(defn create-temp-service
;  ([machine]
;   (create-temp-service machine {}))
;  ([machine {:keys [id] :as config}]
;   (let [machine-id (or id [:temp (next-temp-id) (:id machine)])]
;     (create-service machine (merge config {:id machine-id})))))
;
;(defn subscribe-state [service-id]
;  (ra/cursor sc-state-db service-id))
;(defn subscribe-value
;  ([service-id]
;   (subscribe-value service-id nil))
;  ([service-id values]
;   (ra/cursor sc-state-db (concat (conj service-id :value) values))))
;(defn subscribe-context
;  ([service-id]
;   (subscribe-context service-id nil))
;  ([service-id values]
;   (ra/cursor sc-state-db (concat (conj service-id :context) values))))
;(defn subscribe-value-list [service-id]
;  (let [value (subscribe-value service-id)]
;    (ratom/make-reaction #(gs/value-to-ids @value))))
;(defn subscribe-value? [service-id state-id]
;  (let [vl (subscribe-value-list service-id)]
;    (ratom/make-reaction #(contains? @vl (name state-id)))))
;
;
;
;
;
;
;(defn get-service [service-id]
;  (get-in @sc-service-db service-id))
;


;(defn component-with-machine [machine render-fn & args]
;  (let [service-id (create-temp-service machine)]
;    (ra/create-class
;      {:display-name   (str (:id (gs/state-def-machine machine)))
;       :reagent-render (fn [_comp]
;                         (apply render-fn service-id args))
;       :component-will-unmount (fn[_comp]
;                                 (remove-service service-id))})))

