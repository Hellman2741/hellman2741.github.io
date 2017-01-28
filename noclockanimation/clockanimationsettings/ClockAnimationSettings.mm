#import <Preferences/Preferences.h>

@interface ClockAnimationSettingsListController: PSListController {
}
@end

@implementation ClockAnimationSettingsListController
- (id)specifiers {
	if(_specifiers == nil) {
		_specifiers = [[self loadSpecifiersFromPlistName:@"ClockAnimationSettings" target:self] retain];
	}
	return _specifiers;
}
@end

// vim:ft=objc
