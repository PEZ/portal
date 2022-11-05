(ns portal.ui.viewer.diff
  (:require [lambdaisland.deep-diff2 :refer [diff]]
            [lambdaisland.deep-diff2.diff-impl :as diff]
            [portal.colors :as c]
            [portal.runtime.cson :as cson]
            [portal.ui.commands :as commands]
            [portal.ui.icons :as icons]
            [portal.ui.inspector :as ins]
            [portal.ui.select :as select]
            [portal.ui.styled :as s]
            [portal.ui.theme :as theme]))

(extend-protocol cson/ToJson
  diff/Deletion
  (-to-json [this buffer] (cson/tag buffer "diff/Deletion" (:- this)))

  diff/Insertion
  (-to-json [this buffer] (cson/tag buffer "diff/Insertion" (:+ this)))

  diff/Mismatch
  (-to-json [this buffer] (cson/tag buffer "diff/Mismatch" ((juxt :- :+) this))))

(defn ^:no-doc ->diff [op value]
  (case op
    "diff/Deletion"  (diff/Deletion.  value)
    "diff/Insertion" (diff/Insertion. value)
    "diff/Mismatch"  (let [[a b] value]
                       (diff/Mismatch. a b))
    nil))

(defn diff? [value]
  (or
   (instance? diff/Deletion value)
   (instance? diff/Insertion value)
   (instance? diff/Mismatch value)))

(defn- can-view? [value]
  (or (diff? value)
      (and (coll? value)
           (not (map? value))
           (> (count value) 1))))

(defn inspect-diff [value]
  (let [theme (theme/use-theme)
        bg (ins/get-background)]
    (cond
      (instance? diff/Deletion value)  [ins/inspector (:- value)]
      (instance? diff/Insertion value) [ins/inspector (:+ value)]
      (instance? diff/Mismatch value)  [ins/inspector value]
      :else
      [s/div
       {:style
        {:background (ins/get-background)}}
       [s/div
        {:style
         {:display :flex
          :justify-content :space-between}}
        [s/div
         {:style
          {:flex "1"
           :padding (:padding theme)
           :background (::c/diff-remove theme)
           :border-top-left-radius (:border-radius theme)}}
         [icons/minus-circle {:style {:color bg}}]]
        [s/div
         {:style
          {:padding (:padding theme)
           :color (::c/border theme)
           :border [1 :solid (::c/border theme)]}}
         [icons/exchange-alt]]
        [s/div
         {:style
          {:flex "1"
           :padding (:padding theme)
           :text-align :right
           :background (::c/diff-add theme)
           :border-top-right-radius (:border-radius theme)}}
         [icons/plus-circle {:style {:color bg}}]]]
       [s/div
        {:style
         {:display :flex
          :flex-direction :column
          :gap (:padding theme)
          :padding (:padding theme)
          :border-left [1 :solid (::c/border theme)]
          :border-right [1 :solid (::c/border theme)]
          :border-bottom [1 :solid (::c/border theme)]
          :border-bottom-left-radius (:border-radius theme)
          :border-bottom-right-radius (:border-radius theme)}}
        [ins/dec-depth
         (->> (partition 2 1 value)
              (map-indexed
               (fn [idx [a b]]
                 ^{:key idx}
                 [ins/with-key
                  idx
                  [ins/with-collection
                   [a b]
                   [select/with-position
                    {:row idx :column 0}
                    [ins/inspector (diff a b)]]]])))]]])))

(def viewer
  {:predicate can-view?
   :component inspect-diff
   :name :portal.viewer/diff})

(let [var  #'diff
      name (#'commands/var->name var)]
  (swap! commands/registry
         assoc name (commands/make-command
                     (merge (meta var)
                            {:predicate (fn [& args] (= 2 (count args)))
                             :f var
                             :name name}))))
