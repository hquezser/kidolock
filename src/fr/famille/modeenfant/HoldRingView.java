package fr.famille.modeenfant;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/** Anneau de progression d'appui long autour du cadenas parent (0 → 1). */
public class HoldRingView extends View {
    private final Paint arc = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF box = new RectF();
    private float progress;

    public HoldRingView(Context c) {
        this(c, null);
    }

    public HoldRingView(Context c, AttributeSet attrs) {
        super(c, attrs);
        arc.setStyle(Paint.Style.STROKE);
        arc.setStrokeWidth(Ui.dp(c, 5));
        arc.setStrokeCap(Paint.Cap.ROUND);
        arc.setColor(c.getColor(R.color.primary));
    }

    void setProgress(float p) {
        p = Math.max(0f, Math.min(1f, p));
        if (p != progress) {
            progress = p;
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas c) {
        if (progress <= 0f) return;
        float inset = arc.getStrokeWidth() / 2f + 2f;
        box.set(inset, inset, getWidth() - inset, getHeight() - inset);
        c.drawArc(box, -90f, 360f * progress, false, arc);
    }
}
