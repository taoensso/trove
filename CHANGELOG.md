This project uses [**Break Versioning**](https://www.taoensso.com/break-versioning).

---

# `v1.0.1` (2025-10-11)

- **Dependency**: [on Clojars](https://clojars.org/com.taoensso/trove/versions/1.1.0)
- **Versioning**: [Break Versioning](https://www.taoensso.com/break-versioning)

This is a minor feature release.

##  Since v1.0.0 (2025-08-21)

- \[new] Added `:log-fn` option to `log!` (handy for testing) \[deb6375]

---

# `v1.0.0` (2025-08-21)

- **Dependency**: [on Clojars](https://clojars.org/com.taoensso/trove/versions/1.0.0)
- **Versioning**: [Break Versioning](https://www.taoensso.com/break-versioning)

This is the official **v1 stable release** of Trove 🍾🥳  

If no unexpected issues come up, Trove should basically be "done" at this point. \- [Peter Taoussanis](https://www.taoensso.com)

##  Since v1.0.0-RC2 (2025-07-03)

- \[fix] `:trace` level JS console logging \[6816439]
- \[fix] [#3] Require own macros to support `:refer` from Cljs (@jxonas) \[94c6f84]

---

# `v1.0.0-RC2` (2025-07-03)

- **Dependency**: [on Clojars](https://clojars.org/com.taoensso/trove/versions/1.0.0-RC2)
- **Versioning**: [Break Versioning](https://www.taoensso.com/break-versioning)

- \[fix] Hotfix for broken Cljs console log-fn \[9d0dc55]

---

# `v1.0.0-RC1` (2025-06-25)

- **Dependency**: [on Clojars](https://clojars.org/com.taoensso/trove/versions/1.0.0-RC1)
- **Versioning**: [Break Versioning](https://www.taoensso.com/break-versioning)

- \[new] [#2] More thorough bb testing (@borkdude) \[506477b]
- \[new] Support js/console.trace for trace level output \[50111f7]
- \[new] Improve multi-line support in default output \[682903a]
- \[mod] Drop empty data from default output \[a1446c1]

---

# `v1.0.0-beta2` (2025-06-20)

- **Dependency**: [on Clojars](https://clojars.org/com.taoensso/trove/versions/1.0.0-beta2)
- **Versioning**: [Break Versioning](https://www.taoensso.com/break-versioning)

- \[fix] Fix babashka console output \[c6b177e]
- \[new] Add `:let` support to `log!` \[8bc67af]
- \[new] Add 0-arity log fn constructors \[971674c]
- \[doc] Misc docstring improvements

---
# `v1.0.0-beta1` (2025-06-19)

- **Dependency**: [on Clojars](https://clojars.org/com.taoensso/trove/versions/1.0.0-beta1)
- **Versioning**: [Break Versioning](https://www.taoensso.com/break-versioning)

This is the first public Trove release. The API may still change based on feedback, and feedback very welcome!

Feel free to ping on the Trove [Slack channel](http://taoensso.com/trove/slack) or [GitHub issues](https://github.com/taoensso/trove/issues).

Thank you!! - [Peter Taoussanis](https://www.taoensso.com) 🙏
