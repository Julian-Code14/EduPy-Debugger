<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# EduPy-Debugger Changelog

All notable changes to this project will be documented in this file.
This format follows Keep a Changelog, and versions follow Semantic Versioning (including beta suffixes).

## [1.0.5](https://github.com/Julian-Code14/EduPy-Debugger/compare/v1.0.7...v1.0.5) (2026-04-12)


### ⚠ BREAKING CHANGES

* the WebSocket protocol format has changed to JSON, breaking compatibility with older frontends.

### Features

* **analysis:** show globals in local stop + enrich call stack with arguments ([#221](https://github.com/Julian-Code14/EduPy-Debugger/issues/221)) ([0c95539](https://github.com/Julian-Code14/EduPy-Debugger/commit/0c955393716e3b46f24f2764b166860914ebe3e8))
* **console:** format Python tracebacks into student-friendly messages with location and tips; add tests ([#219](https://github.com/Julian-Code14/EduPy-Debugger/issues/219)) ([1fe3d11](https://github.com/Julian-Code14/EduPy-Debugger/commit/1fe3d11e0efadb2f00839700b5634fa9396ef94e))
* **console:** suppress pydevd startup line and add idle REPL fallback ([#217](https://github.com/Julian-Code14/EduPy-Debugger/issues/217)) ([954b885](https://github.com/Julian-Code14/EduPy-Debugger/commit/954b885fa7aa46af58bd2281d7da88d3c4d56da4))
* migrate debugger communication to JSON-based WebSocket architecture ([ce3ec99](https://github.com/Julian-Code14/EduPy-Debugger/commit/ce3ec9924653ff7cbf59dbaf870f371c3b20dc06))
* **server:** show class diagram in REPL and clear call stack on REPL start ([#222](https://github.com/Julian-Code14/EduPy-Debugger/issues/222)) ([7d4274c](https://github.com/Julian-Code14/EduPy-Debugger/commit/7d4274c876182c748003c6e9d128cec82b39cd60))
* switch debugger communication to JSON-based WebSocket protocol ([a448206](https://github.com/Julian-Code14/EduPy-Debugger/commit/a44820693d2ea1e81815cce075f42d1ab474c060))
* **ui:** add Help/Privacy/Terms/Contact subpages and footer updates ([#226](https://github.com/Julian-Code14/EduPy-Debugger/issues/226)) ([aa26cde](https://github.com/Julian-Code14/EduPy-Debugger/commit/aa26cde81156366bbad56ffca0c42d21f0fea098))
* **ui:** expandable values in variables table (preview + full) ([#223](https://github.com/Julian-Code14/EduPy-Debugger/issues/223)) ([51401cb](https://github.com/Julian-Code14/EduPy-Debugger/commit/51401cb669dfd4892564283f7e73d350a0e285f3))


### Bug Fixes

* **build:** fixed build script for version injection ([2b5f081](https://github.com/Julian-Code14/EduPy-Debugger/commit/2b5f081abc044ce7baf65b177f44b5a528009ae1))
* **build:** fixed build script for version injection ([e97f9e4](https://github.com/Julian-Code14/EduPy-Debugger/commit/e97f9e48d8a02c63f5735e3a70c76b48bce9ac6a))
* **debugger:** enforce global stepping; stabilize frame events; clarify UI ([#224](https://github.com/Julian-Code14/EduPy-Debugger/issues/224)) ([8959575](https://github.com/Julian-Code14/EduPy-Debugger/commit/89595754e99d9b94fe33e8ebc52269b49af27287))
* **docs:** trigger 0.5.2-beta.0 release ([e820f18](https://github.com/Julian-Code14/EduPy-Debugger/commit/e820f18e350d66e13d09bd4272fb0e73254da1a9))
* prepare 1.0.6 release ([#281](https://github.com/Julian-Code14/EduPy-Debugger/issues/281)) ([8d24048](https://github.com/Julian-Code14/EduPy-Debugger/commit/8d240489e3cebe937ac4395ed270417ddc6bf8e9))
* prepare 1.0.7 release ([#287](https://github.com/Julian-Code14/EduPy-Debugger/issues/287)) ([efb2349](https://github.com/Julian-Code14/EduPy-Debugger/commit/efb2349a755064b8083c1afa60b9116315fdd96e))
* **publish:** writeSigningFiles task exception for publishing ([d8b1ee4](https://github.com/Julian-Code14/EduPy-Debugger/commit/d8b1ee44f18d1b42c3389f91634e94b12045451b))
* **publish:** writeSigningFiles task exception for publishing ([#210](https://github.com/Julian-Code14/EduPy-Debugger/issues/210)) ([e24ba28](https://github.com/Julian-Code14/EduPy-Debugger/commit/e24ba28f77b783bd801d0984a512c620e72a64f3))
* **release:** nudge release-please ([5c38f61](https://github.com/Julian-Code14/EduPy-Debugger/commit/5c38f61188433fe75002f154463806c17049d608))
* **release:** nudge release-please ([#206](https://github.com/Julian-Code14/EduPy-Debugger/issues/206)) ([d394212](https://github.com/Julian-Code14/EduPy-Debugger/commit/d394212ffc332b32af7d19f014622429e2d46e90))
* **release:** nudge release-please after config move ([f6dd9a7](https://github.com/Julian-Code14/EduPy-Debugger/commit/f6dd9a709cbae665e178904378f33bdf479f578b))
* **release:** nudge release-please after config move ([#205](https://github.com/Julian-Code14/EduPy-Debugger/issues/205)) ([7dd380d](https://github.com/Julian-Code14/EduPy-Debugger/commit/7dd380df58f3a7efe50faa5430f6e3d61c43bc2e))
* **release:** prepare next release ([016d58b](https://github.com/Julian-Code14/EduPy-Debugger/commit/016d58bce2ee020f8fe7da658eebf834c67b3fd1))
* **release:** prepare next release ([#208](https://github.com/Julian-Code14/EduPy-Debugger/issues/208)) ([b18f200](https://github.com/Julian-Code14/EduPy-Debugger/commit/b18f200396af8d3fdcfd4720213c9cec259a4fe3))
* **release:** trigger release notes for next version ([435a4aa](https://github.com/Julian-Code14/EduPy-Debugger/commit/435a4aadac42daf2e9cccba9e3e2c231c4471a80))
* **release:** trigger release notes for next version ([884150e](https://github.com/Julian-Code14/EduPy-Debugger/commit/884150e23a18d2f4365c68df4aa20e32220393bb))
* **release:** trigger release notes for next version ([#203](https://github.com/Julian-Code14/EduPy-Debugger/issues/203)) ([2e705e5](https://github.com/Julian-Code14/EduPy-Debugger/commit/2e705e5a35fff7fffb461a9c3e2bc032f98676c8))
* **release:** trigger release notes for next version ([#204](https://github.com/Julian-Code14/EduPy-Debugger/issues/204)) ([880c9d7](https://github.com/Julian-Code14/EduPy-Debugger/commit/880c9d7581ad2de9ccc2cc15d1769c6817294335))
* removed unstable or experimental api usages ([b1a63b8](https://github.com/Julian-Code14/EduPy-Debugger/commit/b1a63b850fa3820b128cf331dd1181eddb1d5950))
* removed unstable or experimental api usages ([9692fe8](https://github.com/Julian-Code14/EduPy-Debugger/commit/9692fe8c5a079ec19c5ed6ce1d827424dbc0c5b4))
* trigger new patch release ([13f7704](https://github.com/Julian-Code14/EduPy-Debugger/commit/13f770404ee4808cc6a4f1752ddc3559b4b50906))


### Performance Improvements

* **debugger:** move analysis off EDT, debounce on BGT ([#225](https://github.com/Julian-Code14/EduPy-Debugger/issues/225)) ([11a2031](https://github.com/Julian-Code14/EduPy-Debugger/commit/11a2031fb4316fc79b534e8a23d9f1930eacd687))


### Documentation

* **changelog:** clean up history and standardize format ([105b92f](https://github.com/Julian-Code14/EduPy-Debugger/commit/105b92f9ccb50fa5e560bdf21a58ea14a218dbcd))
* **readme:** start plugin description with ASCII Latin sentence for Marketplace validation ([763a978](https://github.com/Julian-Code14/EduPy-Debugger/commit/763a9788e132b13debd4a38ca61b398fb48bb365))


### Miscellaneous Chores

* cut release ([4ee89ec](https://github.com/Julian-Code14/EduPy-Debugger/commit/4ee89eccc43147cfb81c2951a332b720d1f943f0))
* cut release ([dbede41](https://github.com/Julian-Code14/EduPy-Debugger/commit/dbede418177a9fffa1580fe5fdfeae6e9e49362b))
* cut release ([d9f5e43](https://github.com/Julian-Code14/EduPy-Debugger/commit/d9f5e43179a67a11d8fc3874cf80133f009569b4))
* cut release 0.5.1-beta.0 ([b712fc0](https://github.com/Julian-Code14/EduPy-Debugger/commit/b712fc0e004c31c10bbc79fcf0d79ed80c396148))
* first beta ([5da65e3](https://github.com/Julian-Code14/EduPy-Debugger/commit/5da65e3ff2c5c46d342c0d1bdb68a8f99519b6e9))
* force beta release ([68d79f4](https://github.com/Julian-Code14/EduPy-Debugger/commit/68d79f433975a4eee9263cbf5f8a240e6c751183))
* force release 0.5.2-beta.0 ([a71ded0](https://github.com/Julian-Code14/EduPy-Debugger/commit/a71ded057ceece6e2f00276d1f04afa4fe49afc1))
* **main:** sync dev into main ([#261](https://github.com/Julian-Code14/EduPy-Debugger/issues/261)) ([4775a50](https://github.com/Julian-Code14/EduPy-Debugger/commit/4775a50fe202575332e19161895a08ab6ecc55a3))
* manual release 0.5.2-beta.0 ([8404439](https://github.com/Julian-Code14/EduPy-Debugger/commit/8404439d087a9f3acfca4a843cf9651e7b6a3f11))
* prepare forced release ([e6afc6a](https://github.com/Julian-Code14/EduPy-Debugger/commit/e6afc6a08a0900ebc8c197d7bd9b96949be236da))
* release 0.4.2-beta.0 ([0a323fa](https://github.com/Julian-Code14/EduPy-Debugger/commit/0a323fa916fedc5874c25cb63245ebd3cc5634e3))
* release 0.4.5-beta.0 ([61d7cc7](https://github.com/Julian-Code14/EduPy-Debugger/commit/61d7cc7b1bcc35928ca5c6c7f49fbc3501331c11))
* release 0.4.5-beta.0 ([7dbfcfb](https://github.com/Julian-Code14/EduPy-Debugger/commit/7dbfcfbb94fca6ab076849ab4691cf7d4b23cece))
* release 0.4.5-beta.0 ([dfaedc4](https://github.com/Julian-Code14/EduPy-Debugger/commit/dfaedc40105dc08413ba61e6a47435c418710219))
* release now ([ca6002b](https://github.com/Julian-Code14/EduPy-Debugger/commit/ca6002b820b1237719207ec28b1bf32868dd286d))
* release now ([336dbc0](https://github.com/Julian-Code14/EduPy-Debugger/commit/336dbc017f7ca630f988df0bc13d2f5550a31f9b))
* release now ([84b98b6](https://github.com/Julian-Code14/EduPy-Debugger/commit/84b98b6cf78c4ffd295dc276f0bc82afa2d69a72))
* release now ([#251](https://github.com/Julian-Code14/EduPy-Debugger/issues/251)) ([129c5cd](https://github.com/Julian-Code14/EduPy-Debugger/commit/129c5cd530c3b806031e8df341c883a5791e3b61))
* **release:** force pre-1.0 minor ([61e91f6](https://github.com/Julian-Code14/EduPy-Debugger/commit/61e91f6011d0a7be907fc652c8beaa838f080454))
* **release:** release 1.0.5 ([2dabdcc](https://github.com/Julian-Code14/EduPy-Debugger/commit/2dabdccd58e1f86d17ea5eed72f6c54d392261c7))
* **release:** release 1.0.5 ([1e26b12](https://github.com/Julian-Code14/EduPy-Debugger/commit/1e26b124d4a423fe8c6f2b72c54dedf9ae4126e2))
* trigger 0.4.3 beta release ([443f5ab](https://github.com/Julian-Code14/EduPy-Debugger/commit/443f5ab70298fbbca4db3189a5475e9e711d4b7e))
* trigger release ([09b554b](https://github.com/Julian-Code14/EduPy-Debugger/commit/09b554b71d8eef7504b345e2c7758d3f0ce3d37b))

## [1.0.7](https://github.com/Julian-Code14/EduPy-Debugger/compare/v1.0.6...v1.0.7) (2026-04-12)


### Bug Fixes

* prepare 1.0.7 release ([#287](https://github.com/Julian-Code14/EduPy-Debugger/issues/287)) ([efb2349](https://github.com/Julian-Code14/EduPy-Debugger/commit/efb2349a755064b8083c1afa60b9116315fdd96e))

## [1.0.6](https://github.com/Julian-Code14/EduPy-Debugger/compare/v1.0.5...v1.0.6) (2026-04-12)


### Bug Fixes

* prepare 1.0.6 release ([#281](https://github.com/Julian-Code14/EduPy-Debugger/issues/281)) ([8d24048](https://github.com/Julian-Code14/EduPy-Debugger/commit/8d240489e3cebe937ac4395ed270417ddc6bf8e9))

## [1.0.4](https://github.com/Julian-Code14/EduPy-Debugger/compare/v1.0.3...v1.0.4) (2026-04-02)


### Miscellaneous Chores

* release now ([ca6002b](https://github.com/Julian-Code14/EduPy-Debugger/commit/ca6002b820b1237719207ec28b1bf32868dd286d))

## [1.0.3](https://github.com/Julian-Code14/EduPy-Debugger/compare/v1.0.1...v1.0.3) (2026-04-02)


### Miscellaneous Chores

* release now ([336dbc0](https://github.com/Julian-Code14/EduPy-Debugger/commit/336dbc017f7ca630f988df0bc13d2f5550a31f9b))

## [1.0.1](https://github.com/Julian-Code14/EduPy-Debugger/compare/v1.0.0...v1.0.1) (2026-04-02)


### Miscellaneous Chores

* release now ([84b98b6](https://github.com/Julian-Code14/EduPy-Debugger/commit/84b98b6cf78c4ffd295dc276f0bc82afa2d69a72))

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
