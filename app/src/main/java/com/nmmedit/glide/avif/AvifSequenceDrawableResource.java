package com.nmmedit.glide.avif;

import androidx.annotation.NonNull;

import com.bumptech.glide.load.resource.drawable.DrawableResource;

import libavif.AvifSequenceDrawable;

public class AvifSequenceDrawableResource extends DrawableResource<AvifSequenceDrawable> {

    public AvifSequenceDrawableResource(AvifSequenceDrawable drawable) {
        super(drawable);
    }

    @NonNull
    @Override
    public Class<AvifSequenceDrawable> getResourceClass() {
        return AvifSequenceDrawable.class;
    }


    @Override
    public int getSize() {
        return drawable.getSize();
    }

    @Override
    public void recycle() {
        drawable.stop();
        drawable.destroy();
    }
}
