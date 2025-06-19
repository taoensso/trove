#!/bin/bash

bb -cp src -e "(require '[taoensso.trove :as trove]) (trove/log! {:level :info :id :auth/login :data {:dude 1} :msg :dude})"
