package fr.famille.modeenfant;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * Disque de temps façon « Time Timer » : la part colorée fond au fil de la session.
 * Lisible par un enfant qui ne sait pas lire l'heure. Vert → orange → rouge.
 */
public class TimerView extends View {
    private final Paint back = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF box = new RectF();
    private float fraction = 1f;

    public TimerView(Context c) {
        this(c, null);
    }

    public TimerView(Context c, AttributeSet attrs) {
        super(c, attrs);
        back.setColor(0x26000000);
    }

    void setFraction(float f) {
        f = Math.max(0f, Math.min(1f, f));
        if (f != fraction) {
            fraction = f;
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas c) {
        float s = Math.min(getWidth(), getHeight());
        float l = (getWidth() - s) / 2f, t = (getHeight() - s) / 2f;
        box.set(l, t, l + s, t + s);
        c.drawOval(box, back);
        fill.setColor(fraction > 0.5f ? 0xFF43A047 : fraction > 0.2f ? 0xFFFB8C00 : 0xFFE53935);
        c.drawArc(box, -90f, 360f * fraction, true, fill);
    }
}
