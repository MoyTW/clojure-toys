(ns wysitutwyg.hal.hal-core)

;;;; You can technically have resources without self links!
;;;; This implementation enforces self links because, uh, yeah, you want'm.
;;;; Resources are plain Clojure data structures which map directly to json.

(def link-properties
  #{:href :templated :type :deprecation :name :profile :title :hreflang})
(def reserved-resource-properties
  #{:_links :_embedded})
  
(defn is-resource
  [resource]
  (-> resource :_links :self :href))

(defn new-resource
  [self]
  {:_links {:self {:href self}}})

(defn add-link
  "Takes a link in the form of rel, href, and any number of properties. Adds 
  this to links. The link relation 'curies' is reserved, and cannot be 
  used."
  [resource rel href & property-pairs]
  {:pre [(is-resource resource)
         (not= (keyword rel) :curies) ; "curies" is reserved
         (every? link-properties (map keyword (take-nth 2 property-pairs)))
         (even? (count property-pairs))]}
  (let [link {(keyword rel) 
                (conj {:href href} (apply hash-map property-pairs))}]
    (update-in resource [:_links] #((fnil conj []) % link))))

(defn add-multi-link
  "Adds an array of links associated with one rel. The link relation 'curies'
  is reserved, and cannot be used. Links are maps from link-properties to 
  values. Use like: (add-multi-link resource rel {:href 'www.google.com'}) -
  probably going to change the sig later though."
  [resource rel & links]
  {:pre [(is-resource resource)
         (not= (keyword rel) :curies) ; "curies" is reserved
         (every? link-properties (map keyword (apply map first links)))
         (every? :href links)]}
  nil)

(defn add-curie
  "Adds a reference to the 'curies' section."
  [resource name href & property-pairs]
  {:pre [(is-resource resource)
         (every? link-properties (map keyword (take-nth 2 property-pairs)))
         (even? (count property-pairs))
         (.contains href "{rel}")]}
  nil)


(defn add-property-map
  "Merges property map into the resource. Overwrites with new."
  [resource m]
  {:pre [(is-resource resource)
         (not-any? reserved-resource-properties (map keyword (keys m)))]}
  (merge resource m))

(defn add-properties
  "Adds the specified properties to the resource."
  [resource & property-pairs]
  {:pre [(is-resource resource)
         (not-any? reserved-resource-properties 
                   (map keyword (take-nth 2 property-pairs)))
         (even? (count property-pairs))]}
  (add-property-map resource (apply hash-map property-pairs)))

(defn add-embedded-set
  "Adds the specified embedded resources to the resource, with the name of the
  grouping as rel and contents as args. If there already exists a rel, adds the
  args to the existing rel."
  [resource rel resources]
  {:pre [(is-resource resource)
         (every? #(-> % :_links :self :href) resources)]}
  (update-in resource
             [:_embedded (keyword rel)]
             #(apply (fnil conj []) % resources)))