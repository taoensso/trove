<a href="https://www.taoensso.com/clojure" title="More stuff by @ptaoussanis at www.taoensso.com"><img src="https://www.taoensso.com/open-source.png" alt="Taoensso open source" width="340"/></a>  
[**API**][cljdoc] | [Slack channel][] | Latest release: [v1.1.0](../../releases/tag/v1.1.0) (2025-10-11)

[![Clj tests][Clj tests SVG]][Clj tests URL]
[![Cljs tests][Cljs tests SVG]][Cljs tests URL]
[![Graal tests][Graal tests SVG]][Graal tests URL]
[![bb tests][bb tests SVG]][bb tests URL]

# Trove

### Modern logging facade for Clojure/Script

Trove is a minimal, modern alternative to [tools.logging](https://github.com/clojure/tools.logging) that supports:

- Both traditional **and structured** logging.
- Both Clojure **and ClojureScript**.
- **Richer filtering** capabilities (by namespace, id, level, data, etc.).
- **Dynamic context** for correlating related logs, etc.

It's intended mostly for **library authors** that want to emit rich logging _without_ forcing their users to adopt any particular backend ([Telemere](https://www.taoensso.com/telemere), [Timbre](https://www.taoensso.com/timbre), [μ/log](https://github.com/BrunoBonacci/mulog), [tools.logging](https://github.com/clojure/tools.logging), [SLF4J](https://www.slf4j.org/), etc.).

Trove is tiny (0 deps, ~250 loc), fast, and highly flexible.

## Why structured logging?

- Traditional logging outputs **strings** (messages).
- Structured logging in contrast outputs **data**. It retains **rich data types and (nested) structures** throughout the logging pipeline from logging callsite → filters → middleware → handlers.

A data-oriented pipeline allows **easier filtering**, **transformation**, and **analysis**. It's also often **faster** since it helps avoid unnecessary serialization, and  is well suited to the tools and idioms offered by Clojure and ClojureScript.

## Usage for end users

Trove just **works out the box**: if a library you use depends on Trove, you'll automatically get sensible library logging to `*out*` or the JS console. No need for any config, or even a logging library.

But you may prefer to configure Trove to instead direct logs to your **preferred backend** by calling [`trove/set-log-fn!`](https://cljdoc.org/d/com.taoensso/trove/CURRENT/api/taoensso.trove#set-log-fn!):

```clojure
(ns my-ns
  (:require
   [taoensso.trove.x] ; x ∈ #{console telemere timbre mulog tools-logging slf4j} (default console)
   [taoensso.trove :as trove]))

(trove/set-log-fn! (taoensso.trove.x/get-log-fn {:bridge-ctx? true/false}))
(trove/set-log-fn! nil) ; To noop all `trove/log!` calls
```

> Use `{:bridge-ctx? true}` if you'd like to allow library authors to update your backend's native context when relevant. Requires Telemere, Timbre (except on Babashka), μ/log, or SLF4J with an MDC-capable provider.

You can also easily **write your own log-fn** - see [`trove/*log-fn*`](https://cljdoc.org/d/com.taoensso/trove/CURRENT/api/taoensso.trove#*log-fn*) for details.

## Usage for library authors

### Logging

[Include](../../releases/) Trove in your library deps, then use [`trove/log!`](https://cljdoc.org/d/com.taoensso/trove/CURRENT/api/taoensso.trove#log!) to make your logging calls:

```clojure
(ns my-ns (:require [taoensso.trove :as trove]))

(trove/log! {:level :info, :id :auth/login, :data {:user-id 1234}, :msg "User logged in!"})
```

Trove uses the same map-based logging API as [Telemere](https://www.taoensso.com/telemere). The above expands to roughly:

```clojure
(when-let [log-fn trove/*log-fn*] ; Chosen backend fn
  (log-fn "my-ns" [line column] :info :auth/login ; Callsite info
    {:msg "User logged in!", :data {:user-id 1234}})) ; Payload
```

The end user's chosen backend takes care of filtering and output.

#### What about expensive data?

Structured logging sometimes involves expensive data collection or transformation, e.g.:

```clojure
(trove/log! {:id ::my-event, :data (expensive) ...})
```

That's why Trove automatically delays payload values that need runtime evaluation, allowing the backend to apply filtering *before* paying realization costs. See [`*log-fn*`](https://cljdoc.org/d/com.taoensso/trove/CURRENT/api/taoensso.trove#*log-fn*) for payload details.

### Dynamic context

The [`trove/*ctx*`](https://cljdoc.org/d/com.taoensso/trove/CURRENT/api/taoensso.trove#*ctx*) context map may be attached to `trove/log!` output:

```clojure
(trove/with-ctx+ {:workflow/step "fetch-page"}
  (trove/log! {:id :ingest/fetched, :data {:n 42}}))
```

Utils:

- [`set-root-ctx!`](https://cljdoc.org/d/com.taoensso/trove/CURRENT/api/taoensso.trove#set-root-ctx!) to modify root (default) context
- [`with-ctx`](https://cljdoc.org/d/com.taoensso/trove/CURRENT/api/taoensso.trove#with-ctx) to replace the current context
- [`with-ctx+`](https://cljdoc.org/d/com.taoensso/trove/CURRENT/api/taoensso.trove#with-ctx+) to update the current context

> `log!` also takes `:ctx` and `:ctx+` options to replace or update the context for a single call.

#### Bridging context to the backend (advanced)

The above context belongs to Trove, so affects only `trove/log!` calls.

Some backends have their own equivalent context, which you [can also update](https://cljdoc.org/d/com.taoensso/trove/CURRENT/api/taoensso.trove#with-ctx-bridge) for end-users that choose to opt-in:

```clojure
(trove/with-ctx+ {:workflow/step "fetch-page"}
  (trove/with-ctx-bridge ; Merge Trove context into native backend context
    (fetch-page)))
```

Custom backends can add bridge support with [`trove/add-ctx-bridge`](https://cljdoc.org/d/com.taoensso/trove/CURRENT/api/taoensso.trove#add-ctx-bridge).

## Funding

You can [help support][sponsor] continued work on this project and [others][my work], thank you!! 🙏

## License

Copyright &copy; 2026 [Peter Taoussanis][].  
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
