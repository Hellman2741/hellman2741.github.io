@interface ClockAnimation: NSObject

@end

static NSUserDefaults *Preferences;

static void LoadPreferences() {

    Preferences = [[NSUserDefaults alloc] initWithSuiteName:@"com.cryo.clockanimationsettings"];

    [Preferences registerDefaults:@{
        @"enabled" : @true
    }];

}

@implementation ClockAnimation

+ (void)load {

    CFNotificationCenterAddObserver(CFNotificationCenterGetDarwinNotifyCenter(),
                                NULL,
                                (CFNotificationCallback)LoadPreferences,
                                CFSTR("com.cryo.clockanimationsettings/prefsChanged"),
                                NULL,
                                CFNotificationSuspensionBehaviorDeliverImmediately);
    LoadPreferences();

}

@end

%hook SBClockApplicationIconImageView

-(void)_setAnimating:(bool)animating {
  BOOL enabled = [Preferences boolForKey:@"enabled"];
  %orig(enabled ? FALSE : animating);
}

- (void)_updateUnanimatedWithComponents:(id)val {
  BOOL enabled = [Preferences boolForKey:@"enabled"];
  %orig(enabled ? NULL : val);
}

%end
