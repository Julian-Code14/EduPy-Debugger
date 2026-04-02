<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# EduPy-Debugger Changelog

All notable changes to this project will be documented in this file.
This format follows Keep a Changelog, and versions follow Semantic Versioning (including beta suffixes).

## [1.0.0](https://github.com/Julian-Code14/EduPy-Debugger/compare/v0.5.3-beta.0...v1.0.0) (2026-04-02)


### Features

* **analysis:** show globals in local stop + enrich call stack with arguments ([#221](https://github.com/Julian-Code14/EduPy-Debugger/issues/221)) ([0c95539](https://github.com/Julian-Code14/EduPy-Debugger/commit/0c955393716e3b46f24f2764b166860914ebe3e8))
* **console:** format Python tracebacks into student-friendly messages with location and tips; add tests ([#219](https://github.com/Julian-Code14/EduPy-Debugger/issues/219)) ([1fe3d11](https://github.com/Julian-Code14/EduPy-Debugger/commit/1fe3d11e0efadb2f00839700b5634fa9396ef94e))
* **console:** suppress pydevd startup line and add idle REPL fallback ([#217](https://github.com/Julian-Code14/EduPy-Debugger/issues/217)) ([954b885](https://github.com/Julian-Code14/EduPy-Debugger/commit/954b885fa7aa46af58bd2281d7da88d3c4d56da4))
* **server:** show class diagram in REPL and clear call stack on REPL start ([#222](https://github.com/Julian-Code14/EduPy-Debugger/issues/222)) ([7d4274c](https://github.com/Julian-Code14/EduPy-Debugger/commit/7d4274c876182c748003c6e9d128cec82b39cd60))
* **ui:** add Help/Privacy/Terms/Contact subpages and footer updates ([#226](https://github.com/Julian-Code14/EduPy-Debugger/issues/226)) ([aa26cde](https://github.com/Julian-Code14/EduPy-Debugger/commit/aa26cde81156366bbad56ffca0c42d21f0fea098))
* **ui:** expandable values in variables table (preview + full) ([#223](https://github.com/Julian-Code14/EduPy-Debugger/issues/223)) ([51401cb](https://github.com/Julian-Code14/EduPy-Debugger/commit/51401cb669dfd4892564283f7e73d350a0e285f3))


### Bug Fixes

* **debugger:** enforce global stepping; stabilize frame events; clarify UI ([#224](https://github.com/Julian-Code14/EduPy-Debugger/issues/224)) ([8959575](https://github.com/Julian-Code14/EduPy-Debugger/commit/89595754e99d9b94fe33e8ebc52269b49af27287))


### Performance Improvements

* **debugger:** move analysis off EDT, debounce on BGT ([#225](https://github.com/Julian-Code14/EduPy-Debugger/issues/225)) ([11a2031](https://github.com/Julian-Code14/EduPy-Debugger/commit/11a2031fb4316fc79b534e8a23d9f1930eacd687))

## [0.5.3-beta.0](https://github.com/Julian-Code14/EduPy-Debugger/compare/v0.5.2-beta.0...v0.5.3-beta.0) (2025-10-26)


### Bug Fixes

* **publish:** writeSigningFiles task exception for publishing ([#210](https://github.com/Julian-Code14/EduPy-Debugger/issues/210)) ([e24ba28](https://github.com/Julian-Code14/EduPy-Debugger/commit/e24ba28f77b783bd801d0984a512c620e72a64f3))

## [0.5.2-beta.0](https://github.com/Julian-Code14/EduPy-Debugger/compare/v0.5.1-beta.0...v0.5.2-beta.0) (2025-10-26)


### Bug Fixes

* **release:** prepare next release ([#208](https://github.com/Julian-Code14/EduPy-Debugger/issues/208)) ([b18f200](https://github.com/Julian-Code14/EduPy-Debugger/commit/b18f200396af8d3fdcfd4720213c9cec259a4fe3))


## [0.5.1-beta.0](https://github.com/Julian-Code14/EduPy-Debugger/compare/v0.5.0-beta.0...v0.5.1-beta.0) (2025-10-25)


### Miscellaneous Chores

* cut release 0.5.1-beta.0 ([b712fc0](https://github.com/Julian-Code14/EduPy-Debugger/commit/b712fc0e004c31c10bbc79fcf0d79ed80c396148))

## [0.5.0-beta.0](https://github.com/Julian-Code14/EduPy-Debugger/compare/v0.4.5-beta.0...v0.5.0-beta.0) (2025-10-25)


### ⚠ BREAKING CHANGES

* the WebSocket protocol format has changed to JSON, breaking compatibility with older frontends.

### Features

* migrate debugger communication to JSON-based WebSocket architecture ([ce3ec99](https://github.com/Julian-Code14/EduPy-Debugger/commit/ce3ec9924653ff7cbf59dbaf870f371c3b20dc06))
* switch debugger communication to JSON-based WebSocket protocol ([a448206](https://github.com/Julian-Code14/EduPy-Debugger/commit/a44820693d2ea1e81815cce075f42d1ab474c060))


### Documentation

* **changelog:** clean up history and standardize format ([105b92f](https://github.com/Julian-Code14/EduPy-Debugger/commit/105b92f9ccb50fa5e560bdf21a58ea14a218dbcd))


### Miscellaneous Chores

* **release:** force pre-1.0 minor ([61e91f6](https://github.com/Julian-Code14/EduPy-Debugger/commit/61e91f6011d0a7be907fc652c8beaa838f080454))

## [Unreleased]

## [0.4.5-beta.0](https://github.com/Julian-Code14/EduPy-Debugger/compare/v0.4.4-beta.0...v0.4.5-beta.0) — 2025-08-14
### Bug Fixes
- Removed usage of unstable/experimental IntelliJ APIs to improve compatibility.  
  ([b1a63b8](https://github.com/Julian-Code14/EduPy-Debugger/commit/b1a63b850fa3820b128cf331dd1181eddb1d5950), [9692fe8](https://github.com/Julian-Code14/EduPy-Debugger/commit/9692fe8c5a079ec19c5ed6ce1d827424dbc0c5b4))

## [0.4.4-beta.0](https://github.com/Julian-Code14/EduPy-Debugger/compare/edupy-debugger-v0.4.3-beta.0...edupy-debugger-v0.4.4-beta.0) — 2025-08-14
> Maintenance release (no user-facing changes; release process only).

## [0.4.3-beta.0](https://github.com/Julian-Code14/EduPy-Debugger/compare/edupy-debugger-v0.4.2-beta.0...edupy-debugger-v0.4.3-beta.0) — 2025-08-05
### Bug Fixes
- **build:** Fixed build script for version injection (more robust version resolution in plugin build).  
  ([2b5f081](https://github.com/Julian-Code14/EduPy-Debugger/commit/2b5f081abc044ce7baf65b177f44b5a528009ae1), [e97f9e4](https://github.com/Julian-Code14/EduPy-Debugger/commit/e97f9e48d8a02c63f5735e3a70c76b48bce9ac6a))

## [0.4.2-beta.0](https://github.com/Julian-Code14/EduPy-Debugger/compare/edupy-debugger-v0.4.1-beta.0...edupy-debugger-v0.4.2-beta.0) — 2025-08-05
> Internal maintenance release (no additional changes; release process only).

## [0.4.0-beta.0](https://github.com/Julian-Code14/EduPy-Debugger/compare/edupy-debugger-v0.4.0...edupy-debugger-v0.4.0-beta.0) — 2025-08-04
### Added
- First public **beta** of the EduPy Debugger.
- (Highlights of the beta:)
    - Visual object inspection
    - Class & object diagrams
    - Interactive console I/O
    - Debug controls & thread support (experimental)
    - Browser-based UI, one-click navigation
