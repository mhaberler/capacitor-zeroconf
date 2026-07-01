# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Capacitor plugin for ZeroConf/Bonjour/mDNS service discovery + publishing. Published to npm as `@mhaberler/capacitor-zeroconf-nsd` (fork of trik/capacitor-zeroconf). Targets Capacitor 7. Four platform implementations: iOS, Android, Electron, web. Not a background service — publish/watch stop when the webview is destroyed.

## Build & verify

```bash
yarn build            # clean + docgen + tsc + rollup + electron build → dist/
yarn verify           # builds ios + android + web (CI parity)
yarn verify:android   # cd android && ./gradlew clean build test
yarn verify:ios       # cd ios && pod install && xcodebuild -workspace Plugin.xcworkspace -scheme Plugin
yarn lint             # eslint + prettier --check + swiftlint
yarn fmt              # autofix all three
```

- `yarn build` regenerates the API section of `README.md` via docgen — JSDoc in `src/definitions.ts` is the source of truth for those docs. Edit JSDoc, not the README tables.
- `dist/` is gitignored but **shipped to npm** (`files` field). Stale dist = broken publish. Always `yarn build` before publishing.
- Package manager is yarn 1.x (`packageManager` field). `bun.lock`, `package-lock.json`, `yarn.lock` all present in tree; yarn is canonical for scripts.

## Release

See [LIESMICH.md](LIESMICH.md) for the full npm publish process. Key points: clean tree → `npm version patch` → `yarn build` → `npm publish --access public` (scoped package requires `--access public`) → `git push --tags`.

## Architecture

The TypeScript surface (`src/`) defines one contract implemented natively per platform:

- [src/definitions.ts](src/definitions.ts) — `ZeroConfPlugin` interface + all types. **Single source of truth** for the API and (via JSDoc) the README.
- [src/index.ts](src/index.ts) — `registerPlugin` call wiring the four implementations.
- [src/web.ts](src/web.ts) — web fallback.

Native implementations, all exposing the same JS-facing methods (`getHostname`, `register`, `unregister`, `stop`, `watch`, `unwatch`, `close` + `discover` event):

- iOS — [ios/Plugin/ZeroConfPlugin.swift](ios/Plugin/ZeroConfPlugin.swift) (`@objc(ZeroConfPlugin)`, the Capacitor bridge) delegates to [ios/Plugin/ZeroConf.swift](ios/Plugin/ZeroConf.swift) (NetService logic). `.m`/`.h` register the plugin with the Capacitor runtime.
- Android — `io.trik.capacitor.zeroconf` package: `ZeroConfPlugin.java` (bridge) → `ZeroConf.java` (NsdManager logic) + `ZeroConfServiceWatchCallback.java`.
- Electron — [electron/src/zeroconf.ts](electron/src/zeroconf.ts), uses the `bonjour` npm package (a peer dependency). Built separately by `build-electron`.

When changing the API: edit `src/definitions.ts` first, then mirror the change across all four implementations + run `yarn build` to refresh README.

## iOS naming note

The Pod is `MhaberlerCapacitorZeroconfNsd` (see `.podspec`) but the Objective-C plugin class / Xcode scheme is still `ZeroConfPlugin` / `Plugin`. Don't conflate the npm scope, Pod name, and class name — they differ deliberately.
