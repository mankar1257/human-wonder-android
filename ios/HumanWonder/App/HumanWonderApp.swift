import SwiftUI

@main
struct HumanWonderApp: App {
    @State private var app = AppModel()

    var body: some Scene {
        WindowGroup {
            RootView()
                .environment(app)
        }
    }
}

struct RootView: View {
    @Environment(AppModel.self) private var app
    @Environment(\.scenePhase) private var phase

    var body: some View {
        ZStack {
            HW.space.ignoresSafeArea()
            switch app.screen {
            case .landing:
                LandingView().transition(.opacity)
            case .name:
                NameView().transition(.opacity)
            case .chat:
                ChatView().transition(.opacity)
            case .profile:
                ProfileView().transition(.move(edge: .trailing))
            case .licenses:
                LicensesView().transition(.move(edge: .trailing))
            }
        }
        .animation(.easeInOut(duration: 0.35), value: app.screen)
        .preferredColorScheme(.dark)
        // Text follows the phone's text size, but grows at most one step past the default, so
        // every screen keeps its designed proportions (as the Android app's 1.15x cap).
        .dynamicTypeSize(...DynamicTypeSize.xLarge)
        .onChange(of: phase) { _, value in
            // The phone does not let an app use the graphics chip in the background.
            if value == .background { app.chat.stop() }
        }
    }
}
