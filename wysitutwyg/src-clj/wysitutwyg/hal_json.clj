(ns wysitutwyg.hal-json)

(defn new-resource
  [self]
  {:_links {:self {:href self}}})

(defn add-link
  [resource rel href & args]
  (let [link {rel (conj {:href href} (apply hash-map args))}]
    (update-in resource [:_links] #((fnil conj []) % link))))