<a href="https://www.taoensso.com/clojure" title="More stuff by @ptaoussanis at www.taoensso.com"><img src="https://www.taoensso.com/open-source.png" alt="Taoensso open source" width="340"/></a>  
[**API**][cljdoc] | [Slack channel][] | Latest release: [v1.0.0-RC1](../../releases/tag/v1.0.0-RC1) 🚧 (2025-06-25)

[![Clj tests][Clj tests SVG]][Clj tests URL]
[![Cljs tests][Cljs tests SVG]][Cljs tests URL]
[![Graal tests][Graal tests SVG]][Graal tests URL]
[![bb tests][bb tests SVG]][bb tests URL]

# Trove

### Modern logging facade for Clojure/Script

Trove is a minimal, modern alternative to [tools.logging](https://github.com/clojure/tools.logging) that supports:

- Both traditional **and structured** logging
- Both Clojure **and ClojureScript**
- **Richer filtering** capabilities (by namespace, id, level, data, etc.)

It's TINY (1 macro, 0 deps, ~100 loc), fast, and highly flexible.

It supports any backend including: [Telemere](https://www.taoensso.com/telemere), [Timbre](https://www.taoensso.com/timbre), [μ/log](https://github.com/BrunoBonacci/mulog), [tools.logging](https://github.com/clojure/tools.logging), [SLF4J](https://www.slf4j.org/), etc.

It works great for **library authors** that want to emit rich logging _without_ forcing their users to adopt any particular backend.

## To log

1. Include the (tiny) [dependency](../../releases/) in your project or library.
2. Use `trove/log!` to make your logging calls (see its [docstring](https://cljdoc.org/d/com.taoensso/trove/CURRENT/api/taoensso.trove#log!) for options):

```clojure
(ns my-ns (:require [taoensso.trove :as trove]))

(trove/log! {:level :info, :id :auth/login, :data {:user-id 1234}, :msg "User logged in!"})
```

The above logging call expands to:

```clojure
(when-let [log-fn trove/*log-fn*] ; Chosen backend fn
  (log-fn ... "my-ns" :info :auth/login [line-num column-num]
    {:msg "User logged in!", :data {:user-id 1234}} ...))
```

And the chosen backend then takes care of filtering and output.

## To choose a backend

Just set `trove/*log-fn*` to an appropriate fn (see its [docstring](https://cljdoc.org/d/com.taoensso/trove/CURRENT/api/taoensso.trove#*log-fn*) for fn args).

The default fn prints logs to `*out*` or the JS console.  
Alt fns are also available for some common backends, e.g.:

```clojure
(ns my-ns
  (:require
   [taoensso.trove.x] ; x ∈ #{console telemere timbre mulog tools-logging slf4j} (default console)
   [taoensso.trove :as trove]))

(trove/set-log-fn! (taoensso.trove.x/get-log-fn))
(trove/set-log-fn! nil) ; To noop all `trove/log!` calls
```

It's easy to write your own log-fn if you want to use a different backend or customise anything.

## What about expensive data?

Structured logging sometimes involves expensive data collection or transformation, e.g.:

```clojure
(trove/log! {:id ::my-event, :data (expensive) ...})
```

That's why Trove automatically delays any values that need runtime evaluation, allowing the backend to apply filtering *before* paying realization costs.

This explains the `:lazy_` `{:keys [msg data error kvs]}` arg given to [`truss/*log-fn*`](https://cljdoc.org/d/com.taoensso/trove/CURRENT/api/taoensso.trove#*log-fn*).

## Funding

You can [help support][sponsor] continued work on this project and [others][my work], thank you!! 🙏

## License

Copyright &copy; 2025 [Peter Taoussanis][].  
Licensed under [EPL 1.0](LICENSE.txt) (same as Clojure).

<!-- Common -->

[GitHub releases]: ../../releases
[GitHub issues]:   ../../issues
[GitHub wiki]:     ../../wiki
[Slack channel]:   https://www.taoensso.com/trove/slack

[Peter Taoussanis]: https://www.taoensso.com
[sponsor]:          https://www.taoensso.com/sponsor
[my work]:          https://www.taoensso.com/clojure-libraries

<!-- Project -->

[cljdoc]: https://cljdoc.org/d/com.taoensso/trove/CURRENT/api/taoensso.trove

[Clojars SVG]: https://img.shields.io/clojars/v/com.taoensso/trove.svg
[Clojars URL]: https://clojars.org/com.taoensso/trove

[Clj tests SVG]:   https://github.com/taoensso/trove/actions/workflows/clj-tests.yml/badge.svg
[Clj tests URL]:   https://github.com/taoensso/trove/actions/workflows/clj-tests.yml
[Cljs tests SVG]:  https://github.com/taoensso/trove/actions/workflows/cljs-tests.yml/badge.svg
[Cljs tests URL]:  https://github.com/taoensso/trove/actions/workflows/cljs-tests.yml
[Graal tests SVG]: https://github.com/taoensso/trove/actions/workflows/graal-tests.yml/badge.svg
[Graal tests URL]: https://github.com/taoensso/trove/actions/workflows/graal-tests.yml
[bb tests SVG]:    https://github.com/taoensso/trove/actions/workflows/bb-tests.yml/badge.svg
[bb tests URL]:    https://github.com/taoensso/trove/actions/workflows/bb-tests.yml
