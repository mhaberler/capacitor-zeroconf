Releases from 5.0.0 on are this fork (`@mhaberler/capacitor-zeroconf-nsd`),
which replaced the Android implementation with `NsdManager`. Up to and
including 4.0.0 the history is the upstream project,
[trik/capacitor-zeroconf](https://github.com/trik/capacitor-zeroconf).

## [5.1.0] - 2026-10-09
- Added Swift Package Manager support: the plugin now ships a `Package.swift`
  exposing the `MhaberlerCapacitorZeroconfNsd` product, so it can be installed
  in Capacitor apps that no longer use CocoaPods. Capacitor requires every
  installed plugin to provide a `Package.swift` before an app can generate its
  `CapApp-SPM` package, so one plugin without it blocks the whole app from
  migrating.
- `ZeroConfPlugin` now registers through `CAPBridgedPlugin` in Swift, and the
  `ZeroConfPlugin.m` / `.h` Objective-C bridge has been removed, since an SPM
  target cannot mix Swift and Objective-C sources. Backward compatible:
  `CAPBridgedPlugin` is also the supported registration path under CocoaPods,
  and both the SPM target and the CocoaPods workspace build for
  `generic/platform=iOS`. The JavaScript API is unchanged.
- Fixed: the package listed itself in `dependencies`, so installing it pulled
  in a second copy of itself.
- Fixed: `repository` and `bugs` URLs pointed at the upstream project rather
  than this fork; they also fed `s.homepage` and `s.source` in the podspec.
- Fixed: the README install instructions named the upstream package
  (`capacitor-zeroconf`) instead of `@mhaberler/capacitor-zeroconf-nsd`.
- Fixed: the `ios/PluginTests` target called a `ZeroConf.echo()` that this
  plugin never had — Capacitor template leftovers that could not compile.
  Replaced with a `getHostname()` test, so the target builds and passes again.
- Added a `verify:ios:spm` script, and documented iOS integration and the
  required `Info.plist` keys in the README.

## [5.0.5] - 2026-07-01
- Upgrade Capacitor to v8 and TypeScript to v6

## [5.0.4] - 2026-05-21
- Android: use `proguard-android-optimize.txt` for AGP compatibility

## [5.0.3] - 2025-08-05
- Packaging adjustments while moving to the scoped package name

## [5.0.2] - 2025-08-05
- Renamed the podspec for the scoped package
  (`MhaberlerCapacitorZeroconfNsd`)

## [5.0.1] - 2025-08-05
- First release published as `@mhaberler/capacitor-zeroconf-nsd`

## [5.0.0] - 2025-08-03
- Rewrote the Android implementation on top of `NsdManager`. Not published
  under the scoped package name; 5.0.1 is the first release on npm.

## [4.0.0] - 2025-05-09
- Upgrade Capacitor to v7

## [3.0.0] - 2024-07-30
- Upgrade Capacitor to v6

## [2.0.1] - 2024-07-24
- Fixed a bug in the iOS version when calling watch(), the callback function would not be executed when new services were discovered

## [2.0.0] - 2023-07-14
- Update Capacitor to v5

## [1.0.0] - 2021-11-02

- First version: rewrote the original Cordova plugin as Capacitor plugin and added support for Electron plaform
