# Ever Task Tools - App Icon Specification

## Overview

This document specifies the app icon design requirements for Ever Task Tools across iOS and Android platforms.

---

## Design Concept

### Primary Icon
- **Shape**: Rounded square (iOS) / Adaptive (Android)
- **Background**: Solid teal (#00C9A7)
- **Foreground**: White checkmark with sparkle accent
- **Style**: Flat, modern, minimal

### Visual Elements

#### Background
- **Color**: #00C9A7 (Teal/Aqua)
- **Alternative**: Gradient from #00C9A7 to #00B894 (subtle depth)
- **Treatment**: Solid or very subtle gradient

#### Checkmark
- **Color**: #FFFFFF (Pure white)
- **Style**: Bold, rounded stroke
- **Stroke Width**: 12-16px at 1024x1024
- **Shape**: Classic checkmark with rounded ends
- **Position**: Centered, slightly angled upward

#### Sparkle Accent
- **Color**: #FFFFFF (Pure white) or #FFD700 (Gold)
- **Style**: Four-point star (✨)
- **Size**: ~15% of checkmark size
- **Position**: Upper right of checkmark, slightly overlapping
- **Purpose**: Adds energy and "magic" feeling

---

## iOS Icon Specifications

### Required Sizes

| Size | Usage | Filename |
|------|-------|----------|
| 20pt @2x | Notification | Icon-20@2x.png (40x40) |
| 20pt @3x | Notification | Icon-20@3x.png (60x60) |
| 29pt @2x | Settings | Icon-29@2x.png (58x58) |
| 29pt @3x | Settings | Icon-29@3x.png (87x87) |
| 40pt @2x | Spotlight | Icon-40@2x.png (80x80) |
| 40pt @3x | Spotlight | Icon-40@3x.png (120x120) |
| 60pt @2x | App Store (iPhone) | Icon-60@2x.png (120x120) |
| 60pt @3x | App Store (iPhone) | Icon-60@3x.png (180x180) |
| 76pt @2x | App Store (iPad) | Icon-76@2x.png (152x152) |
| 83.5pt @2x | App Store (iPad Pro) | Icon-83.5@2x.png (167x167) |
| 1024pt @1x | App Store | Icon-1024.png (1024x1024) |

### Design Requirements
- **Corner Radius**: Automatic (system applies)
- **Background**: Fill entire square
- **Safe Zone**: Keep content within 90% of icon area
- **Format**: PNG with transparency for marketing, opaque for app

### iOS Specific Notes
- No transparency in final app icons
- System applies corner radius automatically
- Provide 1024x1024 for App Store listing
- Include dark mode variant (optional)

---

## Android Icon Specifications

### Adaptive Icons (API 26+)

#### Foreground Layer
- **Size**: 108dp x 108dp
- **Safe Zone**: 66dp diameter circle in center
- **Content**: White checkmark + sparkle
- **Format**: Vector drawable (XML) or PNG
- **Filename**: ic_launcher_foreground.xml / .png

#### Background Layer
- **Size**: 108dp x 108dp
- **Content**: Solid teal (#00C9A7)
- **Format**: Color resource or drawable
- **Filename**: ic_launcher_background.xml

### Legacy Icons (Pre-API 26)

| Density | Size | Filename |
|---------|------|----------|
| mdpi | 48x48 | ic_launcher_mdpi.png |
| hdpi | 72x72 | ic_launcher_hdpi.png |
| xhdpi | 96x96 | ic_launcher_xhdpi.png |
| xxhdpi | 144x144 | ic_launcher_xxhdpi.png |
| xxxhdpi | 192x192 | ic_launcher_xxxhdpi.png |

### Google Play Store Icon
- **Size**: 512x512 pixels
- **Format**: PNG or JPEG
- **Shape**: Full square (Google applies mask)
- **Filename**: play_store_icon.png

### Android Specific Notes
- Adaptive icons support different shapes per device
- Foreground must be within 66dp safe zone
- Background can extend to full 108dp
- Legacy icons should match adaptive design

---

## Color Palette

### Primary Colors
| Name | Hex | RGB | Usage |
|------|-----|-----|-------|
| Teal Primary | #00C9A7 | rgb(0, 201, 167) | Background |
| Teal Dark | #00B894 | rgb(0, 184, 148) | Gradient end |
| White | #FFFFFF | rgb(255, 255, 255) | Checkmark |
| Gold Accent | #FFD700 | rgb(255, 215, 0) | Sparkle (alt) |

### Accessibility
- Contrast ratio: 4.5:1 minimum (achieved with white on teal)
- Test with color blindness simulators
- Ensure sparkle is visible against background

---

## Export Guidelines

### iOS Export
1. Create 1024x1024 master in vector format
2. Export all required sizes
3. Use sRGB color space
4. No alpha channel for app icons
5. PNG format

### Android Export
1. Create foreground vector (SVG -> Vector Drawable)
2. Export PNG fallbacks for all densities
3. Create background color resource
4. Test with different mask shapes
5. Provide round icon variant

### File Naming Convention
```
ios/
  Icon-20@2x.png
  Icon-20@3x.png
  ...
  Icon-1024.png

android/
  mipmap-mdpi/ic_launcher.png
  mipmap-hdpi/ic_launcher.png
  ...
  mipmap-xxxhdpi/ic_launcher.png
  mipmap-anydpi-v26/ic_launcher.xml (adaptive)
  drawable/ic_launcher_foreground.xml
  drawable/ic_launcher_background.xml
```

---

## Asset Checklist

### iOS
- [ ] Icon-20@2x.png (40x40)
- [ ] Icon-20@3x.png (60x60)
- [ ] Icon-29@2x.png (58x58)
- [ ] Icon-29@3x.png (87x87)
- [ ] Icon-40@2x.png (80x80)
- [ ] Icon-40@3x.png (120x120)
- [ ] Icon-60@2x.png (120x120)
- [ ] Icon-60@3x.png (180x180)
- [ ] Icon-76@2x.png (152x152)
- [ ] Icon-83.5@2x.png (167x167)
- [ ] Icon-1024.png (1024x1024)

### Android
- [ ] ic_launcher_foreground.xml (vector)
- [ ] ic_launcher_background.xml (color)
- [ ] ic_launcher.xml (adaptive definition)
- [ ] ic_launcher_round.xml (round variant)
- [ ] mipmap-mdpi/ic_launcher.png (48x48)
- [ ] mipmap-hdpi/ic_launcher.png (72x72)
- [ ] mipmap-xhdpi/ic_launcher.png (96x96)
- [ ] mipmap-xxhdpi/ic_launcher.png (144x144)
- [ ] mipmap-xxxhdpi/ic_launcher.png (192x192)
- [ ] play_store_icon.png (512x512)

---

## Design Preview

```
+------------------------+
|                        |
|      +--------+        |
|      |  ✓   ✨|        |
|      +--------+        |
|                        |
|    [Teal Background]   |
|                        |
+------------------------+
```

---

## Additional Notes

### Brand Consistency
- Use same icon across all platforms
- Maintain color consistency
- Sparkle adds brand recognition

### Future Considerations
- Seasonal variants (holiday themes)
- Achievement/level variants
- Dark mode optimized version
- Monochrome version for accessibility

### Tools for Generation
- Figma/Sketch for design
- Xcode Asset Catalog for iOS
- Android Studio Image Asset Studio for Android
- Fastlane for automated generation

---

*Last Updated: 2024*
*Version: 1.0*
