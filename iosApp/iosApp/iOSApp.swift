import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    init() {
        CrashLogger.install()
        KoinIosKt.doInitKoin()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}

/// Minimal, privacy-conscious crash logger.
///
/// No analytics/telemetry are used anywhere in this app by design, which
/// otherwise leaves zero field visibility when something crashes. This
/// installs an `NSSetUncaughtExceptionHandler` that appends crash details to
/// a plain text file in `Library/Caches` (excluded from iCloud backups, and
/// appropriate for transient debug data rather than user data).
///
/// Limitation: `NSSetUncaughtExceptionHandler` only catches Objective-C /
/// Foundation-level uncaught exceptions. Swift runtime traps (e.g. force
/// unwraps, array out-of-bounds, fatalError) and Kotlin/Native crashes are
/// not Objective-C exceptions and will NOT be captured by this handler; a
/// complete solution would require a lower-level signal handler, which is a
/// much larger undertaking than warranted here.
enum CrashLogger {
    private static let logFileName = "crash_log.txt"

    static func install() {
        NSSetUncaughtExceptionHandler { exception in
            let timestamp = ISO8601DateFormatter().string(from: Date())
            let stack = exception.callStackSymbols.joined(separator: "\n")
            let entry = """
            ---
            [\(timestamp)] Uncaught exception
            \(exception.description)
            \(stack)

            """
            appendToLogFile(entry)
        }
    }

    private static func appendToLogFile(_ text: String) {
        guard let cachesURL = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask).first,
              let data = text.data(using: .utf8) else {
            return
        }
        let fileURL = cachesURL.appendingPathComponent(logFileName)

        if let handle = try? FileHandle(forWritingTo: fileURL) {
            defer { try? handle.close() }
            handle.seekToEndOfFile()
            handle.write(data)
        } else {
            try? data.write(to: fileURL, options: .atomic)
        }
    }
}
