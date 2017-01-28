ARCHS = armv7 armv7s arm64
THEOS_DEVICE_IP = localhost
include theos/makefiles/common.mk

TWEAK_NAME = NoClockAnimation
NoClockAnimation_FILES = ClockAnimation.xm
NoClockAnimation_FRAMEWORKS = UIKit
NoClockAnimation_PRIVATE_FRAMEWORKS = Preferences
SDKVERSION = 8.1

NoClockAnimation_CFLAGS = -fobjc-arc
NoClockAnimation_LDFLAGS += -Wl,-segalign,4000

include $(THEOS_MAKE_PATH)/tweak.mk

after-install::
	install.exec "killall -9 SpringBoard"
SUBPROJECTS += clockanimationsettings
include $(THEOS_MAKE_PATH)/aggregate.mk
