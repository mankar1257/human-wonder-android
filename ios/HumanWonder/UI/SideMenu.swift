import SwiftUI

private let selectedFill = Color(argb: 0x1F83FFE5) // teal 12%
private let itemFill = Color(argb: 0x14FFFFFF) // white 8%, the chat's chip fill

/// The side menu: a sheet min(320, screen − 56) wide over a black 60% scrim, with Home, New
/// conversation, search, the conversations by day and the profile row. It opens from the menu
/// button; tapping the scrim or swiping it left closes it.
struct SideMenu: View {
    @Environment(AppModel.self) private var app
    @State private var renaming: Conversation?
    @State private var renameText = ""
    @State private var deleting: Conversation?
    @State private var dragX: CGFloat = 0
    @FocusState private var searchFocused: Bool

    var body: some View {
        GeometryReader { geo in
            let width = min(320, geo.size.width - 56)
            let open = app.menuOpen
            ZStack(alignment: .leading) {
                Color.black
                    .opacity(open ? 0.6 * Double(1 + min(0, dragX) / width) : 0)
                    .ignoresSafeArea()
                    .onTapGesture { close() }
                    .allowsHitTesting(open)
                sheet
                    .frame(width: width)
                    .offset(x: open ? min(0, dragX) : -width - 24)
            }
            .simultaneousGesture(
                DragGesture(minimumDistance: 12)
                    .onChanged { value in
                        if abs(value.translation.width) > abs(value.translation.height) { dragX = value.translation.width }
                    }
                    .onEnded { value in
                        let sideways = abs(value.translation.width) > abs(value.translation.height)
                        if sideways && (value.translation.width < -80 || value.predictedEndTranslation.width < -160) { close() }
                        withAnimation(.easeOut(duration: 0.2)) { dragX = 0 }
                    },
                including: open ? .all : .subviews
            )
        }
        .animation(.easeOut(duration: 0.28), value: app.menuOpen)
        .alert("Rename transmission", isPresented: Binding(get: { renaming != nil }, set: { if !$0 { renaming = nil } })) {
            TextField("Transmission name", text: $renameText)
            Button("Cancel", role: .cancel) {}
            Button("Save") {
                if let c = renaming { app.rename(c.id, to: renameText) }
            }
        }
        .alert("Delete this transmission?", isPresented: Binding(get: { deleting != nil }, set: { if !$0 { deleting = nil } })) {
            Button("Delete", role: .destructive) {
                if let c = deleting { app.delete(c.id) }
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("“\(deleting?.title ?? "")” will be removed from this phone.")
        }
    }

    private var sheet: some View {
        @Bindable var app = app
        let list = app.menuConversations
        return VStack(spacing: 0) {
            BrandLockup()
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, 20)
                .frame(height: 64)

            VStack(spacing: 4) {
                MenuItem(icon: "ic_home", label: "Home", tint: HW.iconMuted, fill: .clear) { app.goHome() }
                MenuItem(icon: "ic_add", label: "New conversation", tint: HW.primary, fill: itemFill) {
                    app.chat.newConversation()
                    close()
                }
            }
            .padding(.horizontal, 12)
            .padding(.top, 4)
            .padding(.bottom, 8)

            HStack(spacing: 10) {
                HWIcon(name: "ic_search", color: HW.inkTertiary, size: 20)
                ZStack(alignment: .leading) {
                    if app.searchQuery.isEmpty {
                        Text("Search your transmissions").hw(HWType.menuLabel).foregroundStyle(HW.placeholder).lineLimit(1)
                    }
                    TextField("", text: $app.searchQuery)
                        .hw(HWType.menuLabel)
                        .foregroundStyle(.white)
                        .tint(HW.primary)
                        .submitLabel(.search)
                        .focused($searchFocused)
                        .accessibilityLabel("Search your transmissions")
                }
            }
            .padding(.horizontal, 16)
            .frame(minHeight: 48)
            .background(RoundedRectangle(cornerRadius: 24, style: .continuous).fill(itemFill))
            .padding(.horizontal, 12)
            .padding(.vertical, 4)

            ScrollView {
                LazyVStack(alignment: .leading, spacing: 2) {
                    if list.isEmpty {
                        Text(app.searchQuery.trimmingCharacters(in: .whitespaces).isEmpty
                             ? "Your conversations with Orbit 412 will appear here."
                             : "No transmissions match “\(app.searchQuery.trimmingCharacters(in: .whitespaces))”.")
                            .hw(HWType.menuLabel)
                            .foregroundStyle(HW.inkTertiary)
                            .padding(.horizontal, 28)
                            .padding(.top, 16)
                    }
                    ForEach(Self.groupByDay(list), id: \.0) { label, group in
                        SectionLabel(text: label)
                            .padding(.horizontal, 28)
                            .padding(.top, 16)
                            .padding(.bottom, 6)
                        ForEach(group) { c in
                            row(c)
                        }
                    }
                }
                .padding(.bottom, 12)
            }
            .scrollDismissesKeyboard(.immediately)

            Button {
                close()
                app.screen = .profile
            } label: {
                let name = app.profile.name.trimmingCharacters(in: .whitespaces).isEmpty ? "Wanderer" : app.profile.name
                HStack(spacing: 12) {
                    InitialAvatar(name: name)
                    VStack(alignment: .leading, spacing: 0) {
                        Text(name).hw(HWType.menuLabelStrong).foregroundStyle(.white).lineLimit(1)
                        Text("Profile & settings").hw(HWType.chatMeta).foregroundStyle(HW.inkSecondary).lineLimit(1)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    HWIcon(name: "ic_chevron_right", color: HW.inkTertiary, size: 20)
                }
                .padding(.leading, 20)
                .padding(.trailing, 16)
                .padding(.vertical, 8)
                .frame(minHeight: 72)
                .overlay(alignment: .top) { Rectangle().fill(Color(argb: 0x14FFFFFF)).frame(height: 1) }
                .contentShape(Rectangle())
            }
            .buttonStyle(PressScale(scale: 0.99))
        }
        .background(
            UnevenRoundedRectangle(bottomTrailingRadius: 24, topTrailingRadius: 24, style: .continuous)
                .fill(HW.sheet)
                .overlay(UnevenRoundedRectangle(bottomTrailingRadius: 24, topTrailingRadius: 24, style: .continuous).fill(HW.glass))
                .overlay(alignment: .trailing) { Rectangle().fill(Color(argb: 0x0FFFFFFF)).frame(width: 1).padding(.vertical, 24) }
                .ignoresSafeArea()
        )
    }

    private func row(_ c: Conversation) -> some View {
        let selected = c.id == app.chat.conversationId
        return HStack(spacing: 0) {
            Text(c.title)
                .hw(HWType.menuLabel)
                .foregroundStyle(selected ? .white : HW.chip)
                .lineLimit(1)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.vertical, 14)
            if selected {
                Menu {
                    Button("Rename") { startRename(c) }
                    Button("Delete", role: .destructive) { deleting = c }
                } label: {
                    HWIcon(name: "ic_more_vert", color: HW.inkSecondary, size: 20)
                        .frame(width: 40, height: 40)
                        .contentShape(Circle())
                }
                .accessibilityLabel("More options")
            }
        }
        .padding(.leading, 16)
        .padding(.trailing, 4)
        .frame(minHeight: 48)
        .background(RoundedRectangle(cornerRadius: 24, style: .continuous).fill(selected ? selectedFill : .clear))
        .contentShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
        .onTapGesture {
            app.chat.open(c.id)
            close()
        }
        .contextMenu {
            Button("Rename") { startRename(c) }
            Button("Delete", role: .destructive) { deleting = c }
        }
        .padding(.horizontal, 12)
        .accessibilityAddTraits(.isButton)
    }

    private func startRename(_ c: Conversation) {
        renameText = c.title
        renaming = c
    }

    private func close() {
        searchFocused = false
        app.menuOpen = false
    }

    /// The conversations by day, newest first: Today, Yesterday, Earlier.
    static func groupByDay(_ list: [Conversation]) -> [(String, [Conversation])] {
        let calendar = Calendar.current
        let today = list.filter { calendar.isDateInToday($0.updatedAt) }
        let yesterday = list.filter { calendar.isDateInYesterday($0.updatedAt) }
        let earlier = list.filter { !calendar.isDateInToday($0.updatedAt) && !calendar.isDateInYesterday($0.updatedAt) }
        return [("Today", today), ("Yesterday", yesterday), ("Earlier", earlier)].filter { !$0.1.isEmpty }
    }
}

/// A 48pt pill: Home, New conversation.
private struct MenuItem: View {
    let icon: String
    let label: String
    let tint: Color
    let fill: Color
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 14) {
                HWIcon(name: icon, color: tint, size: 22)
                Text(label).hw(HWType.menuLabel).foregroundStyle(HW.ink).lineLimit(1)
                Spacer(minLength: 0)
            }
            .padding(.horizontal, 16)
            .frame(minHeight: 48)
            .background(RoundedRectangle(cornerRadius: 24, style: .continuous).fill(fill))
            .contentShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
        }
        .buttonStyle(PressScale(scale: 0.98))
    }
}
