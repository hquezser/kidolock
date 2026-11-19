package fr.famille.modeenfant;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * Anneau de temps (design session) : piste fine + arc coloré à cap rond.
 * Couleur pilotable via {@link #setColorRes} ; sinon dégradé vert → orange → rouge
 * selon la fraction restante.
 */
public class TimerView extends View {
    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arc = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF box = new RectF();
    private float fraction = 1f;
    private int colorRes = 0;

    public TimerView(Context c) {
        this(c, null);
    }

    public TimerView(Context c, AttributeSet attrs) {
        super(c, attrs);
        float w = Ui.dp(c, 9);
        track.setColor(0xFFE3E7F0);
        track.setStyle(Paint.Style.STROKE);
        track.setStrokeWidth(w);
        arc.setStyle(Paint.Style.STROKE);
        arc.setStrokeWidth(w);
        arc.setStrokeCap(Paint.Cap.ROUND);
    }

    void setFraction(float f) {
        f = Math.max(0f, Math.min(1f, f));
        if (f != fraction) {
            fraction = f;
            invalidate();
        }
    }

    /** Force la couleur de l'arc (0 = automatique selon la fraction). */
    void setColorRes(int res) {
        if (res != colorRes) {
            colorRes = res;
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas c) {
        float s = Math.min(getWidth(), getHeight());
        float inset = arc.getStrokeWidth() / 2f + 1f;
        float l = (getWidth() - s) / 2f + inset, t = (getHeight() - s) / 2f + inset;
        box.set(l, t, l + s - 2 * inset, t + s - 2 * inset);
        c.drawOval(box, track);
        int col = colorRes != 0 ? getContext().getColor(colorRes)
                : fraction > 0.5f ? getContext().getColor(R.color.ok)
                : fraction > 0.2f ? getContext().getColor(R.color.accent)
                : getContext().getColor(R.color.warn);
        arc.setColor(col);
        c.drawArc(box, -90f, 360f * fraction, false, arc);
    }
}
