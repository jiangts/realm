(ns realm.doo-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [realm.test-runner]))

(doo-tests 'realm.core-test)
