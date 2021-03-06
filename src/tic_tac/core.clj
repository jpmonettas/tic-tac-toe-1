(ns tic-tac.core
  (:require [clojure.string :as str]
            )
  (:gen-class))


;; TODO: use https://github.com/mikera/core.matrix
(defn columns [matrix]
  "get list of columns in matrix"
   (apply (partial map list) matrix))

(defn is-square [matrix]
  "check if matrix is square"
  (let [col-num (map count matrix)
        complete (apply = col-num)
        is-square (if complete (= (count col-num) (first col-num)) false)
        ]
    is-square)
  )
;; jp
;(defn is-square [matrix]
;  (apply = (conj (map count matrix) (count matrix))))

(defn diagonals [matrix]
  "extract diagonals from matrix"
  (let [index-range (range (count matrix))
        f (fn [i v] (get v i))]
    [(map f index-range matrix)
     (map f (reverse index-range) matrix)]))

(defn line-winner [line]
  "check if someone wins in this line"
  (if (apply = line) (first line) \?)
  )


(defn line-winable [line]
  "check winning opportunities in this line"
  (let [freqs (frequencies line)]
    (if (= (get freqs \?) 1)
      (first (first (filter #(= 2 (val %)) freqs)))
      )
    )
  )

(let [freqs (frequencies [\x \o \x])]
  (if (= (get freqs \?) 1)
    (first (first (filter #(= 2 (val %)) freqs)))
    )
  )
(get (frequencies [\x \? \x]) \?)
(defn get-lines [board]
  "extracts ascending diagonal, desc diag, columns right to left, rows top to bottom
  (we dont need order for playing tic tac toe)"
  (let [h (apply list board)
        v  (into () (columns board))
        [d1 d2] (diagonals board)
        all (conj (concat v h) d1 d2)
        ]
    all (conj (concat v h) d1 d2)
    ))
;(defn winner [plays]
;  (first (filter #(not= % \?)
;                 (map line-winner plays)
;                 ))
;  )
(defn winables [board]
          (filter #(and (not= % \?) (not= % nil) (not= % '()))
                   (map line-winable (get-lines board))
                 )
  )

(defn winner [board]
  (filter #(not= % \?) (map line-winner (get-lines board)))
  )
(defn find-with-val [board val]
  "finds coords of elements with value val on the board"
  (let [indexed-rows (map-indexed (fn [i v] [i v])  board)]
    (reduce concat (filter not-empty (map (fn [[row-index row-val]]
                             (filter not-empty (map-indexed (fn [i v] (if (= v val) [ row-index i ])) row-val))
                             ) indexed-rows))
    ))
  )
;(defn find-with-val [board val]
;  "create all posible scenarios for next move"
;  (let [indexed-rows (map-indexed (fn [i v] [i v])  matrix)]
;    (map (fn [[row-index row-val]]
;           (map-indexed (fn [i v] [(+ (* row-index (count row-val)) i) (= v val)]) row-val)
;           ) indexed-rows)
;    )
;  )
(defn apply-action [action board [r c]]
  (assoc board r (assoc (nth board r) c action))
  )

(defn future-boards [action board condition]
  (let [available-coords (find-with-val board condition)]
    (map (fn [n] [n (apply-action action board n)]) available-coords)
    ))
(defn map-row [row-index row]
  (map-indexed
    (fn [i v]
      [[row-index i] v])
    row)
  )

(defn index-board [board]
  (apply concat (map-indexed
                  map-row
                  board))
  )

(defn first-moves [board player opponent]
  "center/opposite corner/empty corner/empty side"
  (let [indexed (index-board board)
        first-move (= (count (find-with-val board \?)) 9)
        size (count board)
        max (- size 1)
        med (/ max 2)
        center (first (filter #(and (= (first %) (= (second %) \?)) [med med]) indexed))
        empty-corners (map #(first %) (filter #(and (or (= (first %) [0 0])
                                                        (= (first %) [0 max])
                                                        (= (first %) [max 0])
                                                        (= (first %) [max max])
                                                        )
                                                    (= \? (second %)))
                                              indexed))
        [row col :as opponent-corner] (some not-empty (map #(first %) (filter #(and (or (= (first %) [0 0])
                                                                               (= (first %) [0 max])
                                                                               (= (first %) [max 0])
                                                                               (= (first %) [max max])
                                                                               )
                                                                           (= opponent (second %)))
                                                                     indexed)))
        opposite-corner (if opponent-corner (remove #(or (= % [(if (zero? row) max 0) (if (zero? col) max 0)])
                                     (= % [row col]))
                                empty-corners))
        ;[(if (zero? row) max 0) (if (zero? col) max 0)]
        empty-sides (map #(first %) (filter #(and (or (= (first %) [0 med])
                                 (= (first %) [med 0])
                                 (= (first %) [med max])
                                 (= (first %) [max med])
                                 )
                                  (= \? (second %)))
                            indexed))
        ]
    ;; if first move use a corner to increase opponent error prob
    (if first-move
      (rand-nth [[0 0] [0 max] [max 0] [max max]])
      (if center
        ;(println "center")
        (first center)
        ;empty-corners
        (if (not-empty opposite-corner)
          ;(println opposite-corner)
          opposite-corner
          (if (not-empty empty-corners)
            (rand-nth empty-corners)
            (rand-nth empty-sides)
            )
          )
        )
      )
    )
  )

(defn forks [winnable player]
  "find if player makes a fork deducing from winnables [[0 0] {o 1}]"
  (if (= (get (second winnable) player) 2)
    (first winnable)
    )
  )

(defn best-next-move [board player opponent]
  (let [
        my-options  (future-boards player board \?)
        his-options (future-boards opponent board \?)
        winners  (remove nil? (map (fn [n]
                                     (let [coords (first n)
                                           winner (first (winner (second n)))]
                                       (if winner coords))
                                     )
                                   my-options))
        losers  (remove nil? (map (fn [n]
                                    (let [coords (first n)
                                          winner (first (winner (second n)))]
                                      (if winner coords))
                                    )
                                  his-options))
        old-forks (frequencies (remove nil? (winables  board)))
        potential-wins  (remove nil? (map (fn [n]
                                    (let [coords (first n)
                                          winables (winables (second n))]
                                      (if (not-empty winables) [coords  (frequencies winables)]))
                                    )
                                  my-options))
        my-forks (remove nil? (map #(forks % player) potential-wins))
        potential-loses  (remove nil? (map (fn [n]
                                      (let [coords (first n)
                                            winables (winables (second n))]
                                        (if (not-empty winables) [coords (frequencies winables)]))
                                      )
                                    his-options))
        his-forks (remove nil? (map #(forks % opponent) potential-loses))
        ]
    (if (not-empty winners)
      (rand-nth winners)
      (if (not-empty losers)
        (rand-nth losers)
        (if (not-empty my-forks)
          (first my-forks)
          (if (not-empty his-forks)
            (first his-forks)
            (first-moves board player opponent)
            ;(println "first m,oves")
            )
          )
        )
      )
    )
  )
(def empty-board [[\? \? \?]
                  [\? \? \?]
                  [\? \? \?]])

(def board (atom nil))
(def current-player (atom nil))
(def game-on (atom true))
(defn make-a-move [player board coords]
  (let [indexed (index-board board)
        selected (first (filter #(= (first %) coords) indexed))
        ]
    (if (= (second selected) \?)
      (apply-action player board coords)
      )
    )
  )
(-main)
;@board
;;@current-player
;matrix
;(best-next-move matrix \o \x)
;(best-next-move @board \o \x)
;(future-boards \o @board \?)
(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (reset! board empty-board)
  (reset! current-player \x)
  (reset! game-on true)
  (println "")
  (println "introduce coord [r c] USER (x) first")
  (while @game-on
    (println (str "Current player: " @current-player))
    (doall (map println @board))
    (Thread/sleep 1000)
    (let [input (if (= @current-player \x)
                  (str/split (read-line) #" "))
          move (if (= @current-player \x)
                 [(Integer. (re-find #"\d+" (first input))) (Integer. (re-find #"\d+" (second input)))]
                 (best-next-move @board \o \x)
                 )
          next-board (make-a-move @current-player @board move)
          ]
      ;(doall (map println next-board))
      (if next-board
        (do
          (reset! board next-board)
               (let [winner (winner @board)]
                 (if (not-empty winner)
                   (do
                     (println "winner!!!!!")
                     (println winner)
                     (reset! game-on false)
                     )
                   (if (not-empty (find-with-val @board \?))
                     (reset! current-player (if (= @current-player \x) \o \x))
                     (reset! game-on false)
                     )
                   )
                 ))
        (println "wrong move!")
        )

      )
    )



  )
(comment
  (remove #(or (= % '[0 0]) (= % '[max max])) '[[0 0] [0 max] [max 0] [max max]])
  (first-moves forking-matrix \x \o)
  (first-moves empty-matrix \x \o)
  (first-moves sides-matrix \x \o)
  (first-moves start-matrix \o \x)
  (def empty-matrix [[\? \? \?]
                     [\? \? \?]
                     [\? \? \?]])
  (def forking-matrix [[\? \? \o]
                       [\x \? \o]
                       [\? \? \x]])
  (def sides-matrix [[\o \x \o]
                     [\? \x \x]
                     [\x \o \o]])
  (def corner-matrix [[\? \? \x]
                      [\x \o \o]
                      [\o \? \x]])
  (best-next-move start-matrix \x \o)

  (best-next-move sides-matrix \x \o)
  (best-next-move forking-matrix \o \x)
  (def matrix [[\? \? \?]
               [\? \x \?]
               [\? \? \?]])
  (def matrix2 [[\x \o \o]
                [\x \x \?]
                [\x \? \?]])
  (winner matrix2)

  (forks matrix)
  (best-next-move matrix \x \o)

  (best-next-move matrix2 \x \o)
  matrix
  (future-boards \x matrix \?)
  (apply-action \? matrix [0 1])
  (concat '(([1 0] [1 2]) ([2 1] [2 2])) )
(type matrix)
(assoc matrix 0 (assoc (first matrix) 2 \?))
(find-with-val matrix \?)
(map #(apply-action \x matrix %) (find-with-val matrix \?))
(map #(println %) (find-with-val matrix \?))

  ;; using a solving algorithm



  (map #(= % \?) (second matrix))
  (map-indexed (fn [i v] [i (= v \?)]) (second matrix))
  (map-indexed (fn [i v] [i v])  matrix)
  (map-indexed (fn [i v] [i v])  matrix)
  ((fn [[row-index row-val]] (map-indexed (fn [i v] [(+ (* row-index (count row-val)) i) v]) row-val)) [2 [\x \o \x]])
  (let [indexed-rows (map-indexed (fn [i v] [i v])  matrix)]
    (map (fn [[row-index row-val]]
           (map-indexed (fn [i v] [(+ (* row-index (count row-val)) i)  (= v \?)]) row-val)
           ) indexed-rows)
    )
  (let [indexed-rows (map-indexed (fn [i v] [i v])  matrix)]
    (map (fn [[row-index row-val]]
            (map-indexed (fn [i v] [(+ (* row-index (count row-val)) i)  (= v \?)]) row-val)
           ) indexed-rows)
    )

  (let [indexed-rows (map-indexed (fn [i v] [i v])  matrix)]
    (filter not-empty (map (fn [[row-index row-val]]
           (filter second (map-indexed (fn [i v] [(+ (* row-index (count row-val)) i)  (= v \?)]) row-val))
           ) indexed-rows))
    )
  (map (fn [[row-index row-val]] (map-indexed (fn [i v] [(+ (* row-index (count row-val)) i) v]) row-val)) (map-indexed (fn [i v] [i v])  matrix))
  ;(map (fn [n] (map #(= % \?) n))  matrix)
  (keep second (find-with-val matrix \?))

  )
