%hook SBClockApplicationIconImageView

-(void)_setAnimating:(bool)animating {
  %orig(FALSE);
}

- (void)_updateUnanimatedWithComponents:(id)val {
  %orig(NULL);
}

%end
