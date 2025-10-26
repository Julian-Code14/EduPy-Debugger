<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# EduPy-Debugger Changelog

All notable changes to this project will be documented in this file.
This format follows Keep a Changelog, and versions follow Semantic Versioning (including beta suffixes).

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
