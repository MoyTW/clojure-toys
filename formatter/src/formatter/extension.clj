(ns formatter.extension)

(defprotocol FormatterExtension
  (is-active [this])
  (modify-tree [this tree]))