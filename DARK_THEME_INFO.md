# Dark Theme Implementation

## Overview
The app now automatically switches between light and dark themes based on the system dark mode setting.

## Dark Theme Colors

### Primary Colors
- **Primary Green**: `#66BB6A` - Vibrant green for buttons and accents
- **Secondary Orange**: `#FFB74D` - Warm orange for secondary actions
- **Background**: `#121212` - Deep black background
- **Surface**: `#1E1E1E` - Elevated surface for cards
- **Surface Variant**: `#2C2C2C` - Card backgrounds

### Text Colors
- **On Background**: `#E6E6E6` - Soft white for primary text
- **On Surface**: `#E6E6E6` - Soft white for text on surfaces
- **On Surface Variant**: `#B0B0B0` - Gray for secondary text

### Accent Colors
- **Accent Green**: `#81C784` - Light green accents
- **Accent Orange**: `#FFCC80` - Light orange accents
- **Border**: `#3A3A3A` - Borders and dividers
- **Divider**: `#2A2A2A` - Subtle dividers

### Container Colors
- **Primary Container**: `#1B5E20` - Dark green container background

## How It Works

1. **Automatic Detection**: The app uses `isSystemInDarkTheme()` to detect the phone's dark mode setting
2. **Theme Switching**: When dark mode is enabled on the phone, the app automatically switches to the dark color scheme
3. **Status Bar**: The status bar color adapts to match the theme (dark background in dark mode, green in light mode)

## Testing Dark Mode

To test the dark theme:
1. Go to your phone's Settings
2. Enable Dark Mode/Dark Theme
3. Open the FRIZZLY app
4. The app will automatically display in dark mode

To switch back:
1. Disable Dark Mode in phone settings
2. The app will automatically return to light mode

## Files Modified

- `app/src/main/java/com/example/f_rizzly/ui/theme/Color.kt` - Dark theme colors defined
- `app/src/main/java/com/example/f_rizzly/ui/theme/Theme.kt` - Theme switching logic
- `app/src/main/java/com/example/f_rizzly/MainActivity.kt` - Fixed hardcoded colors
- `app/src/main/java/com/example/f_rizzly/CheckoutScreen.kt` - Fixed hardcoded colors

## Theme-Aware Helper Functions

The app uses these composable functions that automatically adapt to the current theme:
- `activeGreen()` - Primary green color
- `lightGreen()` - Light green container
- `inactiveGray()` - Secondary text color
- `lightGray()` - Background color
- `darkText()` - Primary text color
- `orange()` - Secondary/accent color
- `cardBackground()` - Card surface color
- `surfaceVariant()` - Variant surface color
