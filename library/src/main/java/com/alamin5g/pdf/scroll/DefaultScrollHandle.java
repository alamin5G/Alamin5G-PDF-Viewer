package com.alamin5g.pdf.scroll;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.alamin5g.pdf.PDFView;
import com.alamin5g.pdf.viewer.R;

/**
 * Default scroll handle implementation for PDFView (Issue #6).
 * <p>
 * Displays a draggable handle on the right edge (vertical, default) or bottom
 * edge (horizontal) that syncs with and controls the PDF scroll position.
 * Shows a page indicator bubble (e.g. "3/10") during drag.
 * <p>
 * Usage:
 * <pre>
 *   pdfView.scrollHandle(new DefaultScrollHandle(this))
 *           .fromAsset("document.pdf")
 *           .load();
 * </pre>
 *
 * @since 1.0.17
 */
public class DefaultScrollHandle extends FrameLayout {

    private static final String TAG = "DefaultScrollHandle";

    // ---------- child views ----------
    private View handlerBar;
    private TextView indicatorView;

    // ---------- PDFView reference ----------
    private PDFView pdfView;

    // ---------- configuration ----------
    private boolean horizontal = false;

    // ---------- position tracking ----------
    private int handlerPos = 0; // current handler position in pixels

    // ---------- drag state ----------
    private boolean isDragging = false;
    private float touchStart;     // raw Y (vertical) or raw X (horizontal)
    private float dragStartPos;   // handlerPos at drag start

    // ---------- dimensions (pixels, converted from dp) ----------
    private final int handlerBarWidth;
    private final int handlerBarHeight;
    private final int indicatorSize;
    private final int indicatorMargin;

    // ---------- auto-hide ----------
    private Runnable hideIndicatorRunnable;
    private static final long INDICATOR_HIDE_DELAY_MS = 1500;

    // ------------------------------------------------------------------ //
    //  Constructors
    // ------------------------------------------------------------------ //

    public DefaultScrollHandle(Context context) {
        super(context);
        handlerBarWidth  = dpToPx(24);
        handlerBarHeight = dpToPx(100);
        indicatorSize    = dpToPx(56);
        indicatorMargin  = dpToPx(8);
        init();
    }

    /** Create with orientation. {@code horizontal=true} places handle at bottom. */
    public DefaultScrollHandle(Context context, boolean horizontal) {
        super(context);
        this.horizontal   = horizontal;
        handlerBarWidth   = dpToPx(24);
        handlerBarHeight  = dpToPx(100);
        indicatorSize     = dpToPx(56);
        indicatorMargin   = dpToPx(8);
        init();
    }

    public DefaultScrollHandle(Context context, AttributeSet attrs) {
        super(context, attrs);
        handlerBarWidth  = dpToPx(24);
        handlerBarHeight = dpToPx(100);
        indicatorSize    = dpToPx(56);
        indicatorMargin  = dpToPx(8);
        init();
    }

    public DefaultScrollHandle(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        handlerBarWidth  = dpToPx(24);
        handlerBarHeight = dpToPx(100);
        indicatorSize    = dpToPx(56);
        indicatorMargin  = dpToPx(8);
        init();
    }

    // ------------------------------------------------------------------ //
    //  Initialisation
    // ------------------------------------------------------------------ //

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void init() {
        setClipChildren(false);

        // Layout params for this handle inside PDFView (FrameLayout)
        if (horizontal) {
            setLayoutParams(new FrameLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    indicatorSize + indicatorMargin + handlerBarWidth,
                    Gravity.BOTTOM));
        } else {
            setLayoutParams(new FrameLayout.LayoutParams(
                    indicatorSize + indicatorMargin + handlerBarWidth,
                    LayoutParams.MATCH_PARENT,
                    Gravity.END));
        }

        // ---- handler bar ----
        handlerBar = new View(getContext());
        int barW = horizontal ? handlerBarHeight : handlerBarWidth;
        int barH = horizontal ? handlerBarWidth  : handlerBarHeight;
        handlerBar.setLayoutParams(new LayoutParams(barW, barH));
        handlerBar.setBackgroundResource(horizontal
                ? R.drawable.default_scroll_handle_bottom
                : R.drawable.default_scroll_handle_right);

        // ---- page indicator ----
        indicatorView = new TextView(getContext());
        indicatorView.setLayoutParams(new LayoutParams(indicatorSize, indicatorSize));

        GradientDrawable indicatorBg = new GradientDrawable();
        indicatorBg.setCornerRadius(dpToPx(8));
        indicatorBg.setColor(0xFF6C7A89);
        indicatorView.setBackground(indicatorBg);
        indicatorView.setTextColor(Color.WHITE);
        indicatorView.setTextSize(12);
        indicatorView.setGravity(Gravity.CENTER);
        indicatorView.setVisibility(GONE);

        addView(handlerBar);
        addView(indicatorView);

        // ---- touch handling on handler bar ----
        handlerBar.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return handleTouch(event);
            }
        });

        // ---- auto-hide runnable ----
        hideIndicatorRunnable = new Runnable() {
            @Override
            public void run() {
                indicatorView.setVisibility(GONE);
                handlerBar.setAlpha(0.4f);
            }
        };

        handlerBar.setAlpha(0.4f);
    }

    // ------------------------------------------------------------------ //
    //  Layout — position children based on handlerPos
    // ------------------------------------------------------------------ //

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        layoutHandle();
    }

    private void layoutHandle() {
        if (horizontal) {
            int barLeft = handlerPos;
            int barTop  = indicatorSize + indicatorMargin;
            handlerBar.layout(barLeft, barTop,
                    barLeft + handlerBar.getMeasuredWidth(),
                    barTop  + handlerBar.getMeasuredHeight());

            int indLeft = handlerPos
                    + (handlerBar.getMeasuredWidth() - indicatorView.getMeasuredWidth()) / 2;
            indicatorView.layout(indLeft, 0,
                    indLeft + indicatorView.getMeasuredWidth(),
                    indicatorView.getMeasuredHeight());
        } else {
            int barLeft = indicatorSize + indicatorMargin;
            int barTop  = handlerPos;
            handlerBar.layout(barLeft, barTop,
                    barLeft + handlerBar.getMeasuredWidth(),
                    barTop  + handlerBar.getMeasuredHeight());

            int indTop = handlerPos
                    + (handlerBar.getMeasuredHeight() - indicatorView.getMeasuredHeight()) / 2;
            indicatorView.layout(0, indTop,
                    indicatorView.getMeasuredWidth(),
                    indTop + indicatorView.getMeasuredHeight());
        }
    }

    // ------------------------------------------------------------------ //
    //  Efficient position update (no full layout)
    // ------------------------------------------------------------------ //

    private void setHandlerPos(int newPos) {
        int maxPos;
        if (horizontal) {
            maxPos = Math.max(0, getWidth() - handlerBar.getMeasuredWidth());
        } else {
            maxPos = Math.max(0, getHeight() - handlerBar.getMeasuredHeight());
        }
        newPos = Math.max(0, Math.min(maxPos, newPos));

        if (newPos != handlerPos) {
            int delta = newPos - handlerPos;
            handlerPos = newPos;

            if (horizontal) {
                handlerBar.offsetLeftAndRight(delta);
                indicatorView.offsetLeftAndRight(delta);
            } else {
                handlerBar.offsetTopAndBottom(delta);
                indicatorView.offsetTopAndBottom(delta);
            }
        }
    }

    // ------------------------------------------------------------------ //
    //  Touch handling
    // ------------------------------------------------------------------ //

    private boolean handleTouch(MotionEvent event) {
        if (pdfView == null) return false;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isDragging    = true;
                touchStart    = horizontal ? event.getRawX() : event.getRawY();
                dragStartPos  = handlerPos;
                handlerBar.setAlpha(1.0f);
                indicatorView.setVisibility(VISIBLE);
                removeCallbacks(hideIndicatorRunnable);
                updateIndicatorText();
                return true;

            case MotionEvent.ACTION_MOVE:
                if (isDragging) {
                    float current = horizontal ? event.getRawX() : event.getRawY();
                    float delta   = current - touchStart;
                    setHandlerPos((int) (dragStartPos + delta));

                    // Calculate scroll progress from handle position
                    int maxPos;
                    if (horizontal) {
                        maxPos = Math.max(1, getWidth() - handlerBar.getMeasuredWidth());
                    } else {
                        maxPos = Math.max(1, getHeight() - handlerBar.getMeasuredHeight());
                    }
                    float progress = (float) handlerPos / maxPos;

                    pdfView.setPositionOffset(progress, false);
                    updateIndicatorText();
                    return true;
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isDragging) {
                    isDragging = false;
                    removeCallbacks(hideIndicatorRunnable);
                    postDelayed(hideIndicatorRunnable, INDICATOR_HIDE_DELAY_MS);
                    return true;
                }
                break;
        }
        return false;
    }

    private void updateIndicatorText() {
        if (pdfView != null) {
            int page  = pdfView.getCurrentPage() + 1; // 1-based display
            int total = pdfView.getPageCount();
            indicatorView.setText(page + "/" + total);
        }
    }

    // ------------------------------------------------------------------ //
    //  Public API — called by PDFView
    // ------------------------------------------------------------------ //

    /**
     * Sync handle position with scroll offset (called by PDFView during scroll/fling).
     *
     * @param offset scroll progress 0..1
     */
    public void setScroll(float offset) {
        if (isDragging) return;

        int maxPos;
        if (horizontal) {
            if (getWidth() == 0) return;
            maxPos = Math.max(0, getWidth() - handlerBar.getMeasuredWidth());
        } else {
            if (getHeight() == 0) return;
            maxPos = Math.max(0, getHeight() - handlerBar.getMeasuredHeight());
        }

        setHandlerPos((int) (offset * maxPos));
    }

    /** Set the PDFView reference for scroll callbacks. */
    public void setPDFView(PDFView pdfView) {
        this.pdfView = pdfView;
    }

    /** Show the handle. */
    public void show() {
        setVisibility(VISIBLE);
        handlerBar.setAlpha(0.4f);
    }

    /** Hide the handle. */
    public void hide() {
        setVisibility(GONE);
    }

    /** Cleanup when PDFView is recycled. */
    public void destroy() {
        removeCallbacks(hideIndicatorRunnable);
        pdfView = null;
    }

    /** Whether the handle is currently being dragged. */
    public boolean isDragging() {
        return isDragging;
    }
}
