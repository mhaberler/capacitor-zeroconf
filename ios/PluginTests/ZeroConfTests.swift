import XCTest
@testable import Plugin

// This target is built by the CocoaPods Xcode project, where the framework
// module is named Plugin. It is excluded from Package.swift.
class ZeroConfTests: XCTestCase {
    func testGetHostnameIsNotEmpty() {
        let implementation = ZeroConf()
        XCTAssertFalse(implementation.getHostname().isEmpty)
    }
}
