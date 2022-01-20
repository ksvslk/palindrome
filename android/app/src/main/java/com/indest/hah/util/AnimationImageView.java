package com.indest.hah.util;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

public class AnimationImageView extends AppCompatImageView
{
    public AnimationImageView(Context context)
    {
        super(context);
    }

    public AnimationImageView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public AnimationImageView(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setBackgroundResource(@DrawableRes int resId)
    {
        super.setBackgroundResource(resId);
        startAnimation(getBackground());
    }

    @Override
    public void setBackgroundDrawable(Drawable background)
    {
        super.setBackgroundDrawable(background);
        startAnimation(background);
    }

    @Override
    public void setImageResource(@DrawableRes int resId)
    {
        super.setImageResource(resId);
        startAnimation(getDrawable());
    }

    @Override
    public void setImageDrawable(@Nullable Drawable drawable)
    {
        super.setImageDrawable(drawable);
        startAnimation(drawable);
    }

    protected void startAnimation(Drawable drawable)
    {
        if (drawable instanceof AnimationDrawable)
        {
            ((AnimationDrawable) drawable).start();
        }
    }
}