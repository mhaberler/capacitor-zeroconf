# capacitor-zeroconf

Capacitor ZeroConf plugin

This plugin allows you to browse and publish ZeroConf/Bonjour/mDNS services from applications developed using Ionic's Capacitor.

This is not a background service. When the cordova view is destroyed/terminated, publish and watch operations are stopped.

Android, iOS and [Electron](https://github.com/capacitor-community/electron) platforms are supported.

The has been ported from [Cordova ZeroConf Plugin](https://github.com/becvert/cordova-plugin-zeroconf).

## Install

```bash
npm install @mhaberler/capacitor-zeroconf-nsd
npx cap sync
```

or

```bash
yarn add @mhaberler/capacitor-zeroconf-nsd
yarn cap sync
```

On iOS both **CocoaPods and Swift Package Manager** are supported — `cap sync`
wires up whichever your app already uses, with no extra steps. See
[iOS integration](#ios-integration) for requirements and details.

## API

<docgen-index>

* [`addListener('discover', ...)`](#addlistenerdiscover-)
* [`getHostname()`](#gethostname)
* [`register(...)`](#register)
* [`unregister(...)`](#unregister)
* [`stop()`](#stop)
* [`watch(...)`](#watch)
* [`unwatch(...)`](#unwatch)
* [`close()`](#close)
* [Interfaces](#interfaces)
* [Type Aliases](#type-aliases)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### addListener('discover', ...)

```typescript
addListener(eventName: 'discover', listenerFunc: (result: ZeroConfWatchResult) => void) => Promise<PluginListenerHandle>
```

| Param              | Type                                                                                     |
| ------------------ | ---------------------------------------------------------------------------------------- |
| **`eventName`**    | <code>'discover'</code>                                                                  |
| **`listenerFunc`** | <code>(result: <a href="#zeroconfwatchresult">ZeroConfWatchResult</a>) =&gt; void</code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt;</code>

--------------------


### getHostname()

```typescript
getHostname() => Promise<{ hostname: string; }>
```

**Returns:** <code>Promise&lt;{ hostname: string; }&gt;</code>

--------------------


### register(...)

```typescript
register(request: ZeroConfRegisterRequest) => Promise<void>
```

| Param         | Type                                                                        |
| ------------- | --------------------------------------------------------------------------- |
| **`request`** | <code><a href="#zeroconfregisterrequest">ZeroConfRegisterRequest</a></code> |

--------------------


### unregister(...)

```typescript
unregister(request: ZeroConfUnregisterRequest) => Promise<void>
```

| Param         | Type                                                                            |
| ------------- | ------------------------------------------------------------------------------- |
| **`request`** | <code><a href="#zeroconfunregisterrequest">ZeroConfUnregisterRequest</a></code> |

--------------------


### stop()

```typescript
stop() => Promise<void>
```

--------------------


### watch(...)

```typescript
watch(request: ZeroConfWatchRequest, callback?: ZeroConfWatchCallback | undefined) => Promise<CallbackID>
```

| Param          | Type                                                                    |
| -------------- | ----------------------------------------------------------------------- |
| **`request`**  | <code><a href="#zeroconfwatchrequest">ZeroConfWatchRequest</a></code>   |
| **`callback`** | <code><a href="#zeroconfwatchcallback">ZeroConfWatchCallback</a></code> |

**Returns:** <code>Promise&lt;string&gt;</code>

--------------------


### unwatch(...)

```typescript
unwatch(request: ZeroConfUnwatchRequest) => Promise<void>
```

| Param         | Type                                                                  |
| ------------- | --------------------------------------------------------------------- |
| **`request`** | <code><a href="#zeroconfwatchrequest">ZeroConfWatchRequest</a></code> |

--------------------


### close()

```typescript
close() => Promise<void>
```

--------------------


### Interfaces


#### PluginListenerHandle

| Prop         | Type                                      |
| ------------ | ----------------------------------------- |
| **`remove`** | <code>() =&gt; Promise&lt;void&gt;</code> |


#### ZeroConfService

| Prop                | Type                                    |
| ------------------- | --------------------------------------- |
| **`domain`**        | <code>string</code>                     |
| **`type`**          | <code>string</code>                     |
| **`name`**          | <code>string</code>                     |
| **`port`**          | <code>number</code>                     |
| **`hostname`**      | <code>string</code>                     |
| **`ipv4Addresses`** | <code>string[]</code>                   |
| **`ipv6Addresses`** | <code>string[]</code>                   |
| **`txtRecord`**     | <code>{ [key: string]: string; }</code> |


#### ZeroConfRegisterRequest

| Prop        | Type                                    |
| ----------- | --------------------------------------- |
| **`port`**  | <code>number</code>                     |
| **`props`** | <code>{ [key: string]: string; }</code> |


#### ZeroConfUnregisterRequest

| Prop       | Type                |
| ---------- | ------------------- |
| **`name`** | <code>string</code> |


#### ZeroConfWatchRequest

| Prop         | Type                |
| ------------ | ------------------- |
| **`type`**   | <code>string</code> |
| **`domain`** | <code>string</code> |


### Type Aliases


#### ZeroConfWatchResult

<code>{ action: <a href="#zeroconfwatchaction">ZeroConfWatchAction</a>; service: <a href="#zeroconfservice">ZeroConfService</a>; }</code>


#### ZeroConfWatchAction

<code>'added' | 'removed' | 'resolved'</code>


#### ZeroConfWatchCallback

<code>(event: <a href="#zeroconfwatchresult">ZeroConfWatchResult</a>): void</code>


#### CallbackID

<code>string</code>


#### ZeroConfUnwatchRequest

<code><a href="#zeroconfwatchrequest">ZeroConfWatchRequest</a></code>

</docgen-api>

## iOS integration

The iOS plugin ships for **both** CocoaPods and Swift Package Manager. `npx cap
sync` picks the one your app already uses; nothing has to be configured either
way.

| | How it resolves | Minimum |
|---|---|---|
| CocoaPods | `MhaberlerCapacitorZeroconfNsd.podspec`, sources from `ios/Plugin/**` | iOS 15.0, Swift 5.1 |
| Swift Package Manager | `Package.swift`, product `MhaberlerCapacitorZeroconfNsd` | iOS 15.0, swift-tools 5.9 |

The SPM package depends on
[capacitor-swift-pm](https://github.com/ionic-team/capacitor-swift-pm) `from:
"8.0.0"`. The Capacitor CLI rewrites that version to match the Capacitor
release your app is on, so the plugin follows your app rather than pinning you.

### Required app configuration

Service discovery needs these keys in the app's `Info.plist`, or iOS 14+ will
silently return no results:

```xml
<key>NSLocalNetworkUsageDescription</key>
<string>Explain here why your app browses the local network.</string>
<key>NSBonjourServices</key>
<array>
    <string>_myservice._tcp</string>
</array>
```

`NSBonjourServices` must list **every** service type you pass to `watch()`;
types that are absent are not discoverable. No App ID capability is needed —
local-network access is granted purely through these keys.

### Why the package name is what it is

The SPM package and its library product are both named
`MhaberlerCapacitorZeroconfNsd`. That is not cosmetic: the Capacitor CLI derives
that identifier from the npm package id and writes it into the consuming app's
`CapApp-SPM/Package.swift` as both the package and the product name. Renaming
either one breaks linking in every app that installs this plugin.

## Changes in 5.1.0

- **Swift Package Manager support.** Adds `Package.swift`, so the plugin can be
  installed in Capacitor apps that have moved off CocoaPods. Capacitor requires
  *every* installed plugin to provide a `Package.swift` before an app's
  `CapApp-SPM` package can be generated, so a single plugin without it blocks
  the whole app from migrating.
- **Swift-native plugin registration.** `ZeroConfPlugin` now conforms to
  `CAPBridgedPlugin`, declaring `identifier`, `jsName` and its methods in Swift.
  The `ZeroConfPlugin.m` / `.h` Objective-C bridge is removed, because an SPM
  target cannot mix Swift and Objective-C sources.

  This is **backward compatible** — `CAPBridgedPlugin` is the supported
  registration path under CocoaPods too, and both the SPM target and the
  CocoaPods workspace are verified to build for `generic/platform=iOS`. The
  JavaScript API is unchanged.
- **Fixed:** the package listed *itself* in `dependencies`, so installing it
  pulled in a second copy of itself.
- **Fixed:** `repository` and `bugs` URLs pointed at the upstream project this
  was forked from rather than at this fork. They also fed `s.homepage` and
  `s.source` in the podspec.
- **Fixed:** the install instructions above named the wrong package.
- **Fixed:** the `ios/PluginTests` target called a `ZeroConf.echo()` that this
  plugin never had — leftover Capacitor template code that could not compile.
  Replaced with a `getHostname()` test, so the target now builds and passes.
- Added a `verify:ios:spm` script that builds the SPM target directly.

## Development

```bash
yarn install
yarn build              # docgen + tsc + rollup (regenerates the API docs above)
yarn verify:ios         # CocoaPods: pod install + build the Plugin workspace
yarn verify:ios:spm     # Swift Package Manager: build the SPM target
yarn verify:android
yarn lint
```

The `## API` section of this file is generated by `docgen` from the JSDoc
comments in `src/definitions.ts`; edit those and re-run `yarn build` rather than
editing the generated block.
