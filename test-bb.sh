#!/bin/bash

bb -cp src -e "(require '[taoensso.trove :as trove]) (trove/log! {:level :info :id :auth/login :data {:user-id 1234} :msg \"User logged in!\"})"
