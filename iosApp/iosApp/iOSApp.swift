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

/// Purely local crash log in `Library/Caches` (excluded from iCloud backups) — this app has no
/// analytics/telemetry by design. Only catches Objective-C/Foundation exceptions, not Swift traps
/// (force-unwraps, fatalError) or Kotlin/Native crashes.
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
