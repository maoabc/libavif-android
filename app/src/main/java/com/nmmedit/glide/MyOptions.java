package com.nmmedit.glide;

import com.bumptech.glide.load.Option;

public class MyOptions {

    public static final Option<Boolean> DISABLE_ANIMATION = Option.memory(
            "com.nmmedit.glide.animated.DisableAnimation", false);

    public static final Option<Boolean> LOOP_ONCE = Option.memory(
            "com.nmmedit.glide.animated.LoopOnce", true);
}
