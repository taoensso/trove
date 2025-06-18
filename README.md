<a href="https://www.taoensso.com/clojure" title="More stuff by @ptaoussanis at www.taoensso.com"><img src="https://www.taoensso.com/open-source.png" alt="Taoensso open source" width="340"/></a>  
[**API**][cljdoc] | [**Wiki**][GitHub wiki] | [Latest releases](#latest-releases) | [Slack channel][]

# Trove

🚧 **Under construction**: this library isn't ready for use yet! 🚧

### Modern logging facade for Clojure/Script

Trove is a minimal, modern alternative to [tools.logging](https://github.com/clojure/tools.logging) that supports:

- Both traditional **and structured** logging
- Both Clojure **and ClojureScript**
- **Richer filtering** capabilities (by namespace, id, level, data, etc.)

It's TINY (1 fn, 0 deps, ~100 loc), fast, and highly flexible.

It supports ANY backend including: [Telemere](https://www.taoensso.com/telemere), [Timbre](https://www.taoensso.com/timbre), [μ/log](https://github.com/BrunoBonacci/mulog), [tools.logging](https://github.com/clojure/tools.logging), [SLF4J](https://www.slf4j.org/), a custom fn, etc.

And it works great for **library authors** that want to emit rich logging _without_ forcing their users to adopt any particular backend:

- Library authors include the tiny [dep][Clojars URL] in their lib, then make their logging calls with the [Trove API](TODO). 
- Library users can then [easily choose](TODO) their preferred backend.

### Quick example

```clojure
(require '[taoensso.trove :as trove :refer [log!]])

;; Logging call:
(log! {:level :info, :id :auth/user-login, :data {:user-name user-name}, :msg "User logged in!"})

;; Above logging call expands to:
(when-let [log-fn trove/*log-fn*] ; Configured backend fn
  (log-fn ... "my.namespace" :info :auth/user-login [line-num column-num]
    {:msg "User logged in!", :data {:user-name user-name}} ...))

;; The configured *log-fn* then takes care of filtering and output...
```

- Use [`log!`](TODO) for logging calls
- Set [`*log-fn*`](TODO) to configure the backend

## Latest release/s

- `YYYY-MM-DD` `vX.Y.Z`: [release info](../../releases/tag/vTODO)

[![Clj tests][Clj tests SVG]][Clj tests URL]
[![Cljs tests][Cljs tests SVG]][Cljs tests URL]
[![Graal tests][Graal tests SVG]][Graal tests URL]

See [here][GitHub releases] for earlier releases.

## Documentation

- [Wiki][GitHub wiki] (getting started, usage, etc.)
- API reference via [cljdoc][cljdoc]
- Support via [Slack channel][] or [GitHub issues][]

## Funding

You can [help support][sponsor] continued work on this project, thank you!! 🙏

## License

Copyright &copy; 2025 [Peter Taoussanis][].  
Licensed under [EPL 1.0](LICENSE.txt) (same as Clojure).

<!-- Common -->

[GitHub releases]: ../../releases
[GitHub issues]:   ../../issues
[GitHub wiki]:     ../../wiki
[Slack channel]: https://www.taoensso.com/trove/slack

[Peter Taoussanis]: https://www.taoensso.com
[sponsor]:          https://www.taoensso.com/sponsor

<!-- Project -->

[cljdoc]: https://cljdoc.org/d/com.taoensso/trove/

[Clojars SVG]: https://img.shields.io/clojars/v/com.taoensso/trove.svg
[Clojars URL]: https://clojars.org/com.taoensso/trove

[Clj tests SVG]:  https://github.com/taoensso/trove/actions/workflows/clj-tests.yml/badge.svg
[Clj tests URL]:  https://github.com/taoensso/trove/actions/workflows/clj-tests.yml
[Cljs tests SVG]:  https://github.com/taoensso/trove/actions/workflows/cljs-tests.yml/badge.svg
[Cljs tests URL]:  https://github.com/taoensso/trove/actions/workflows/cljs-tests.yml
[Graal tests SVG]: https://github.com/taoensso/trove/actions/workflows/graal-tests.yml/badge.svg
[Graal tests URL]: https://github.com/taoensso/trove/actions/workflows/graal-tests.yml