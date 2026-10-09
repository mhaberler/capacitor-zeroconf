// swift-tools-version: 5.9
import PackageDescription

// The package and product names must match the name the Capacitor CLI derives
// from the npm package id (@mhaberler/capacitor-zeroconf-nsd ->
// MhaberlerCapacitorZeroconfNsd); it writes both into the app's
// CapApp-SPM/Package.swift.
let package = Package(
    name: "MhaberlerCapacitorZeroconfNsd",
    platforms: [.iOS(.v15)],
    products: [
        .library(
            name: "MhaberlerCapacitorZeroconfNsd",
            targets: ["ZeroConfPlugin"])
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", from: "8.0.0")
    ],
    targets: [
        .target(
            name: "ZeroConfPlugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm")
            ],
            path: "ios/Plugin",
            exclude: ["Info.plist"])
    ]
)
