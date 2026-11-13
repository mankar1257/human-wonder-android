import SwiftUI
import UIKit
import ImageIO

/// A frame from Orbit's archive under an answer: an astronaut photograph of Earth, or a short loop
/// from the station's time-lapses. It fades in once the words have arrived, says where it is from,
/// and opens full screen (with NASA's credit) when tapped. Same design as the Android FrameCard.
struct FrameCard: View {
    let frame: Frame
    @State private var open = false
    @State private var appeared = false
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Button { open = true } label: {
                Color.clear
                    .aspectRatio(3.0 / 2.0, contentMode: .fit)
                    .overlay { FramePicture(frame: frame, fill: true, still: reduceMotion) }
                    .overlay(alignment: .topLeading) {
                        if frame.isLoop {
                            Text("Time-lapse")
                                .hw(HWType.chatMeta)
                                .foregroundStyle(HW.ink)
                                .padding(.horizontal, 8)
                                .padding(.vertical, 3)
                                .background(Capsule().fill(Color.black.opacity(0.55)))
                                .padding(10)
                        }
                    }
                    .background(HW.bubble)
                    .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
                    .overlay(RoundedRectangle(cornerRadius: 20, style: .continuous).stroke(HW.glassBorder, lineWidth: 1))
                    .frame(maxWidth: 520)
            }
            .buttonStyle(PressScale(scale: 0.98))
            .accessibilityLabel("\(frame.caption), a frame from Orbit's archive" + (frame.isLoop ? ", time-lapse" : ""))
            .accessibilityHint("Opens it full screen")

            Text("From Orbit’s archive · \(frame.caption)")
                .hw(HWType.chatMeta)
                .foregroundStyle(HW.inkSecondary)
                .lineLimit(2)
        }
        .padding(.top, 14)
        .opacity(appeared ? 1 : 0)
        .scaleEffect(appeared ? 1 : 0.97, anchor: .top)
        .onAppear { withAnimation(.easeOut(duration: 0.7)) { appeared = true } }
        .fullScreenCover(isPresented: $open) { FrameViewer(frame: frame) }
    }
}

/// The picture full screen: whole, on black, with its caption and NASA's credit.
private struct FrameViewer: View {
    let frame: Frame
    @Environment(\.dismiss) private var dismiss
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            FramePicture(frame: frame, fill: false, still: reduceMotion)
                .ignoresSafeArea()
                .accessibilityLabel(frame.caption)
            VStack(spacing: 0) {
                HStack {
                    Spacer()
                    IconCircle(icon: "ic_close", label: "Close", color: HW.ink) { dismiss() }
                }
                .padding(4)
                Spacer()
                VStack(alignment: .leading, spacing: 4) {
                    Text(frame.caption).hw(HWType.chatTitle).foregroundStyle(HW.ink)
                    Text("From Orbit’s archive. " + (frame.isLoop ? "Time-lapse from frames \(frame.source). " : "Frame \(frame.source). ")
                         + FrameCatalog.shared.credit + ".")
                        .hw(HWType.chatMeta)
                        .foregroundStyle(HW.inkSecondary)
                        .fixedSize(horizontal: false, vertical: true)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, 20)
                .padding(.vertical, 16)
                .background(Color.black.opacity(0.6))
            }
        }
        .contentShape(Rectangle())
        .onTapGesture { dismiss() }
        .dynamicTypeSize(...DynamicTypeSize.xLarge)
        .preferredColorScheme(.dark)
    }
}

/// Draws a frame: photos decoded to screen size off the main thread, loops played frame by frame
/// while on screen (held on its first frame when Reduce Motion is on, as on Android).
struct FramePicture: View {
    let frame: Frame
    let fill: Bool
    let still: Bool
    @State private var photo: UIImage?
    @State private var loop: LoopSource?

    var body: some View {
        Group {
            if frame.isLoop {
                if let loop {
                    if still {
                        picture(loop.image(at: 0))
                    } else {
                        TimelineView(.animation(minimumInterval: loop.delay)) { t in
                            picture(loop.image(at: Int(t.date.timeIntervalSinceReferenceDate / loop.delay) % max(loop.count, 1)))
                        }
                    }
                } else {
                    Color.clear
                }
            } else if let photo {
                picture(photo.cgImage)
            } else {
                Color.clear
            }
        }
        .task(id: frame.id) {
            if frame.isLoop {
                guard loop == nil, let url = frame.url else { return }
                loop = await Task.detached(priority: .userInitiated) { LoopSource(url: url) }.value
            } else {
                guard photo == nil, let url = frame.url else { return }
                let side = await MainActor.run { UIScreen.main.bounds.width * UIScreen.main.scale }
                photo = await Task.detached(priority: .userInitiated) { FramePhotos.load(url, maxPixels: max(side, 1080)) }.value
            }
        }
    }

    @ViewBuilder
    private func picture(_ image: CGImage?) -> some View {
        if let image {
            Image(decorative: image, scale: 1)
                .resizable()
                .interpolation(.high)
                .aspectRatio(contentMode: fill ? .fill : .fit)
        } else {
            Color.clear
        }
    }
}

/// Decoded photos, so scrolling back up does not decode them again.
enum FramePhotos {
    private static let cache: NSCache<NSURL, UIImage> = {
        let c = NSCache<NSURL, UIImage>()
        c.totalCostLimit = 24 * 1024 * 1024
        return c
    }()

    static func load(_ url: URL, maxPixels: CGFloat) -> UIImage? {
        if let hit = cache.object(forKey: url as NSURL) { return hit }
        guard let src = CGImageSourceCreateWithURL(url as CFURL, nil) else { return nil }
        let options: [CFString: Any] = [
            kCGImageSourceCreateThumbnailFromImageAlways: true,
            kCGImageSourceCreateThumbnailWithTransform: true,
            kCGImageSourceThumbnailMaxPixelSize: Int(maxPixels),
            kCGImageSourceShouldCacheImmediately: true,
        ]
        guard let cg = CGImageSourceCreateThumbnailAtIndex(src, 0, options as CFDictionary) else { return nil }
        let image = UIImage(cgImage: cg)
        cache.setObject(image, forKey: url as NSURL, cost: cg.width * cg.height * 4)
        return image
    }
}

/// An animated WebP read one frame at a time (only the frame on screen is kept in memory).
final class LoopSource: @unchecked Sendable {
    let count: Int
    /// Seconds per frame.
    let delay: Double
    private let source: CGImageSource
    private var cachedIndex = -1
    private var cached: CGImage?
    private let lock = NSLock()

    init?(url: URL) {
        guard let src = CGImageSourceCreateWithURL(url as CFURL, nil) else { return nil }
        source = src
        count = CGImageSourceGetCount(src)
        guard count > 0 else { return nil }
        var d = 1.0 / 12.0
        if let props = CGImageSourceCopyPropertiesAtIndex(src, 0, nil) as? [CFString: Any],
           let webp = props[kCGImagePropertyWebPDictionary] as? [CFString: Any],
           let t = (webp[kCGImagePropertyWebPUnclampedDelayTime] ?? webp[kCGImagePropertyWebPDelayTime]) as? Double, t > 0.01 {
            d = t
        }
        delay = d
    }

    func image(at index: Int) -> CGImage? {
        lock.lock()
        defer { lock.unlock() }
        if index == cachedIndex { return cached }
        cached = CGImageSourceCreateImageAtIndex(source, index, [kCGImageSourceShouldCacheImmediately: true] as CFDictionary)
        cachedIndex = index
        return cached
    }
}
