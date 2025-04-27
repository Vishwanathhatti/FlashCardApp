package com.example.flashcards;

import android.graphics.Camera;
import android.graphics.Matrix;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Transformation;

public class ViewAnimator {

    public static void flipTransition(View rootView, View frontView, View backView, boolean showFront) {
        FlipAnimator animator = new FlipAnimator(frontView, backView);
        animator.setDuration(300);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());

        if (showFront) {
            frontView.setVisibility(View.GONE);
            backView.setVisibility(View.VISIBLE);
        } else {
            frontView.setVisibility(View.VISIBLE);
            backView.setVisibility(View.GONE);
        }

        rootView.startAnimation(animator);
    }

    private static class FlipAnimator extends Animation {
        private final Camera camera;
        private final View fromView;
        private final View toView;
        private float centerX;
        private float centerY;
        private final boolean forward;

        public FlipAnimator(View fromView, View toView) {
            this.fromView = fromView;
            this.toView = toView;
            this.forward = fromView.getVisibility() == View.VISIBLE;
            this.camera = new Camera();

            setDuration(300);
            setFillAfter(false);
            setInterpolator(new AccelerateDecelerateInterpolator());
        }

        @Override
        public void initialize(int width, int height, int parentWidth, int parentHeight) {
            super.initialize(width, height, parentWidth, parentHeight);
            centerX = width / 2f;
            centerY = height / 2f;
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            final double radians = Math.PI * interpolatedTime;
            float degrees = (float) (180.0 * radians / Math.PI);

            if (interpolatedTime >= 0.5f) {
                degrees -= 180f;
                fromView.setVisibility(View.GONE);
                toView.setVisibility(View.VISIBLE);
            }

            if (!forward) {
                degrees = -degrees;
            }

            final Matrix matrix = t.getMatrix();
            camera.save();
            camera.rotateY(degrees);
            camera.getMatrix(matrix);
            camera.restore();

            matrix.preTranslate(-centerX, -centerY);
            matrix.postTranslate(centerX, centerY);
        }
    }
}