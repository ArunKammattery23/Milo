package life.milo.companion;

import android.content.Context;
import android.graphics.*;
import android.view.View;

/** Small code-drawn pet; static poses respect reduced-motion preferences automatically. */
final class PetFaceView extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private String state="happy";
    PetFaceView(Context context){super(context);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
    void setExpression(String value){state=value;invalidate();}
    String expression(){return state;}
    void oval(Canvas c,float l,float t,float r,float b,int color){paint.setStyle(Paint.Style.FILL);paint.setColor(color);c.drawOval(l,t,r,b,paint);}
    void stroke(Canvas c,Path p,int color,float width){paint.setStyle(Paint.Style.STROKE);paint.setColor(color);paint.setStrokeWidth(width);paint.setStrokeCap(Paint.Cap.ROUND);c.drawPath(p,paint);paint.setStyle(Paint.Style.FILL);}
    @Override protected void onDraw(Canvas c){super.onDraw(c);c.save();float scale=Math.min(getWidth()/100f,getHeight()/82f);c.translate((getWidth()-100*scale)/2,(getHeight()-82*scale)/2);c.scale(scale,scale);
        oval(c,13,71,88,79,0xffd6e2cb);oval(c,16,18,84,77,state.equals("off")?0xffc0ccb5:0xffacd58c);
        oval(c,36,51,66,74,0xffc6e4a9);oval(c,46,2,65,23,0xff58945c);
        oval(c,12,33,29,58,0xff9bc67d);oval(c,72,33,88,58,0xff9bc67d);
        boolean closed=state.equals("calm")||state.equals("off")||state.equals("celebrate");
        float h=closed?1.3f:state.equals("curious")||state.equals("listening")?5:3.5f;
        oval(c,33,39-h,39,39+h,0xff293c30);oval(c,61,39-h,67,39+h,0xff293c30);
        if(state.equals("playful"))oval(c,61,37,69,40,0xffacd58c);
        oval(c,25,46,38,52,0xffecb3a0);oval(c,64,46,77,52,0xffecb3a0);
        Path mouth=new Path();mouth.moveTo(42,50);
        if(state.equals("speaking")||state.equals("curious")){oval(c,46,49,55,60,0xff355b43);}
        else {mouth.quadTo(50,state.equals("comfort")||state.equals("concerned")?53:state.equals("off")?50:63,58,50);stroke(c,mouth,0xff293c30,2.5f);}
        if(state.equals("thinking")){oval(c,72,13,77,18,0xff58945c);oval(c,82,7,86,11,0xff58945c);}
        if(state.equals("listening")){Path wave=new Path();wave.moveTo(89,30);wave.quadTo(99,42,89,53);stroke(c,wave,0xff58945c,2.5f);}
        if(state.equals("concerned")||state.equals("comfort")){Path brows=new Path();brows.moveTo(31,30);brows.lineTo(39,28);brows.moveTo(60,28);brows.lineTo(68,30);stroke(c,brows,0xff355b43,2);}
        c.restore();
    }
}
