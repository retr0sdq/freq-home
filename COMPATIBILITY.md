# FREQ 0.2.1 installation compatibility

The user reported the Play Protect sensitive-data installation block, with Learn more and no Install anyway option. Google documents notification-listener declarations as a possible trigger for this block for internet-sideloaded apps in selected markets.

This build genuinely removes notification-reading functionality: no NotificationListenerService subclass, no BIND_NOTIFICATION_LISTENER_SERVICE declaration, no notification-content bridge actions, and no notification access settings prompts. The side panel remains for FREQ’s own alerts and release status. It does not read notifications from other apps.

Package ID and signing certificate are unchanged; version code increases to 3. Install over the existing app without uninstalling to retain local data. Other launcher fixes and the verified APK updater remain. No Play Protect setting changes are required by this build.

Removing the documented potential trigger does not constitute Google approval and does not guarantee installation on this device. If it remains blocked, the exact warning and developer review/appeal are needed.
