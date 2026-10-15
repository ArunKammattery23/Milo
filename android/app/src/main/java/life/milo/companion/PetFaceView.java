package life.milo.companion;

import android.content.Context;
import android.graphics.*;
import android.os.SystemClock;
import android.view.View;

/**
 * Milo as a small code-drawn fairy: fluffy lavender hair, sparkling eyes, petal dress and glowing wings.
 * Every expression is drawn (not a picture), so it can blink, flutter, talk, listen and think.
 * With Android animations off it stays still.
 */
final class PetFaceView extends View {
    static final float W=240,H=244;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path=new Path(),mouth=new Path();
    private final Path hairBack=new Path(),hairCap=new Path(),locks=new Path(),sideLocks=new Path(),ahoge=new Path();
    private final Path wingTop=new Path(),wingLow=new Path(),veins=new Path(),skirt=new Path(),underSkirt=new Path(),petals=new Path(),bowLoops=new Path();
    private final Shader hair,face,wing,iris,skirtFill,blush,glow;
    private final ColorFilter sleepy;
    private String state="happy";
    private float level,shownLevel;private boolean talking;private long nextBlink=SystemClock.uptimeMillis()+2500;
    private final boolean motion;
    private final Runnable frame=new Runnable(){public void run(){invalidate();if(isAttachedToWindow()&&getVisibility()==VISIBLE)postDelayed(this,busy()?33:50);}};

    PetFaceView(Context context){super(context);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        float scale=android.provider.Settings.Global.getFloat(context.getContentResolver(),android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,1f);motion=scale>0;
        paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);
        hair=new RadialGradient(80,44,130,new int[]{0xfff6f8ff,0xffc9d5fa,0xff9fb2ec},new float[]{0,0.42f,1},Shader.TileMode.CLAMP);
        face=new RadialGradient(100,98,62,new int[]{0xfffff8f3,0xfffde6dc},null,Shader.TileMode.CLAMP);
        wing=new LinearGradient(-10,80,88,210,new int[]{0xeeffffff,0xccebeaff,0xb3c9c1f5},null,Shader.TileMode.CLAMP);
        iris=new LinearGradient(0,-15,0,15,new int[]{0xff141c4f,0xff2c4aa8,0xff6f9cf0},new float[]{0,0.55f,1},Shader.TileMode.CLAMP);
        skirtFill=new LinearGradient(0,166,0,214,new int[]{0xffe1ebfe,0xffa7c0ef},null,Shader.TileMode.CLAMP);
        blush=new RadialGradient(0,0,12,new int[]{0xd8ff97b0,0x00ff97b0},null,Shader.TileMode.CLAMP);
        glow=new RadialGradient(100,130,128,new int[]{0x55efe9ff,0x00efe9ff},null,Shader.TileMode.CLAMP);
        ColorMatrix grey=new ColorMatrix();grey.setSaturation(0.15f);sleepy=new ColorMatrixColorFilter(grey);
        // Fluffy bob: overlapping rounds read as soft hair; it ends at the chin so wings and dress show.
        for(float[] o:new float[][]{{36,24,164,146},{50,16,112,66},{88,16,150,66},{24,62,72,146},{128,62,176,146},{18,116,52,148},{148,116,182,148}})hairBack.addOval(o[0],o[1],o[2],o[3],Path.Direction.CW);
        // Soft bangs: a cap whose lower edge hangs in rounded lobes.
        float[][] edge={{162,102},{153,92},{141,93},{128,83},{114,87},{101,78},{87,85},{73,82},{61,92},{48,95},{38,106}};
        hairCap.moveTo(38,106);hairCap.cubicTo(26,52,62,26,100,26);hairCap.cubicTo(138,26,174,52,162,104);
        for(int i=0;i<edge.length-1;i++)hairCap.quadTo((edge[i][0]+edge[i+1][0])/2,Math.max(edge[i][1],edge[i+1][1])+9,edge[i+1][0],edge[i+1][1]);
        hairCap.close();
        // Wisps and strands drawn over the bangs for texture.
        locks.moveTo(96,36);locks.quadTo(74,54,66,86);locks.moveTo(102,36);locks.quadTo(108,58,106,80);locks.moveTo(106,36);locks.quadTo(130,52,140,86);
        locks.moveTo(34,96);locks.quadTo(30,120,36,140);locks.moveTo(166,96);locks.quadTo(170,120,164,140);
        sideLocks.moveTo(48,84);sideLocks.cubicTo(32,106,34,138,42,156);sideLocks.quadTo(52,146,56,126);sideLocks.quadTo(54,104,62,92);sideLocks.close();
        sideLocks.moveTo(152,84);sideLocks.cubicTo(168,106,166,138,158,156);sideLocks.quadTo(148,146,144,126);sideLocks.quadTo(146,104,138,92);sideLocks.close();
        ahoge.moveTo(96,30);ahoge.cubicTo(88,8,108,-4,122,6);ahoge.cubicTo(110,6,101,14,106,30);ahoge.close();
        // Left wings, spreading past the hair; the right ones are mirrored.
        wingTop.moveTo(88,150);wingTop.cubicTo(70,108,22,66,-6,80);wingTop.cubicTo(-24,92,-12,140,28,150);wingTop.quadTo(60,156,88,154);wingTop.close();
        wingLow.moveTo(88,156);wingLow.cubicTo(56,158,0,164,6,196);wingLow.cubicTo(12,220,60,206,90,162);wingLow.close();
        veins.moveTo(88,151);veins.quadTo(46,110,4,86);veins.moveTo(86,153);veins.quadTo(42,140,-6,124);veins.moveTo(86,159);veins.quadTo(48,174,18,198);
        underSkirt.moveTo(78,174);underSkirt.quadTo(56,194,48,212);underSkirt.quadTo(100,224,152,212);underSkirt.quadTo(144,194,122,174);underSkirt.close();
        skirt.moveTo(82,166);skirt.lineTo(118,166);skirt.quadTo(142,184,154,206);
        float[] hem={154,128,100,72,46};for(int i=0;i<4;i++)skirt.quadTo((hem[i]+hem[i+1])/2,220,hem[i+1],206);
        skirt.quadTo(58,184,82,166);skirt.close();
        for(float[] p:new float[][]{{88,168,72,204},{100,168,100,210},{112,168,128,204}}){petals.moveTo(p[0]-5,p[1]);petals.quadTo(p[2]-10,(p[1]+p[3])/2,p[2],p[3]);petals.quadTo(p[2]+10,(p[1]+p[3])/2,p[0]+5,p[1]);petals.close();}
        bowLoops.moveTo(100,158);bowLoops.quadTo(84,145,82,155);bowLoops.quadTo(82,167,100,159);bowLoops.close();
        bowLoops.moveTo(100,158);bowLoops.quadTo(116,145,118,155);bowLoops.quadTo(118,167,100,159);bowLoops.close();
    }

    void setExpression(String value){if(value==null||value.equals(state))return;state=value;invalidate();}
    String expression(){return state;}
    /** Loudness from 0 to 1: the microphone while listening (waves) or Milo's own voice while talking (mouth). */
    void setLevel(float value){level=Math.max(0,Math.min(1,value));}
    /** While talking, the mouth follows Milo's voice and the rest of the face keeps the current emotion. */
    void setTalking(boolean value){if(talking!=value){talking=value;if(!value)level=0;invalidate();}}
    boolean talking(){return talking;}
    private boolean busy(){return talking||state.equals("angry")||state.equals("speaking")||state.equals("listening")||state.equals("thinking")||state.equals("celebrate")||state.equals("excited")||state.equals("laughing");}
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();if(motion){removeCallbacks(frame);post(frame);}}
    @Override protected void onDetachedFromWindow(){removeCallbacks(frame);super.onDetachedFromWindow();}
    @Override protected void onVisibilityChanged(View v,int visibility){super.onVisibilityChanged(v,visibility);if(motion&&visibility==VISIBLE&&isAttachedToWindow()){removeCallbacks(frame);post(frame);}}

    private void fill(Canvas c,Path p,int color){paint.setShader(null);paint.setStyle(Paint.Style.FILL);paint.setColor(color);c.drawPath(p,paint);}
    private void fill(Canvas c,Path p,Shader s){paint.setShader(s);paint.setStyle(Paint.Style.FILL);paint.setColor(0xff000000);c.drawPath(p,paint);paint.setShader(null);}
    private void oval(Canvas c,float l,float t,float r,float b,int color){paint.setShader(null);paint.setStyle(Paint.Style.FILL);paint.setColor(color);c.drawOval(l,t,r,b,paint);}
    private void oval(Canvas c,float l,float t,float r,float b,Shader s){paint.setShader(s);paint.setStyle(Paint.Style.FILL);paint.setColor(0xff000000);c.drawOval(l,t,r,b,paint);paint.setShader(null);}
    private void stroke(Canvas c,Path p,int color,float width){paint.setShader(null);paint.setStyle(Paint.Style.STROKE);paint.setColor(color);paint.setStrokeWidth(width);c.drawPath(p,paint);paint.setStyle(Paint.Style.FILL);}
    private void strokeOval(Canvas c,float l,float t,float r,float b,int color,float width){paint.setShader(null);paint.setStyle(Paint.Style.STROKE);paint.setColor(color);paint.setStrokeWidth(width);c.drawOval(l,t,r,b,paint);paint.setStyle(Paint.Style.FILL);}
    /** Four-point twinkle. */
    private void sparkle(Canvas c,float x,float y,float r,int color){path.reset();path.moveTo(x,y-r);path.quadTo(x,y,x+r,y);path.quadTo(x,y,x,y+r);path.quadTo(x,y,x-r,y);path.quadTo(x,y,x,y-r);path.close();fill(c,path,color);}
    /** Five-point star. */
    private void star(Canvas c,float x,float y,float r,int color){path.reset();for(int i=0;i<10;i++){double a=-Math.PI/2+i*Math.PI/5;float rr=i%2==0?r:r*0.45f;float px=x+(float)Math.cos(a)*rr,py=y+(float)Math.sin(a)*rr;if(i==0)path.moveTo(px,py);else path.lineTo(px,py);}path.close();fill(c,path,color);}
    private void heart(Canvas c,float x,float y,float r,int color){path.reset();path.moveTo(x,y+r);path.cubicTo(x-r*1.6f,y-r*0.2f,x-r*0.7f,y-r*1.4f,x,y-r*0.4f);path.cubicTo(x+r*0.7f,y-r*1.4f,x+r*1.6f,y-r*0.2f,x,y+r);path.close();fill(c,path,color);}

    @Override protected void onDraw(Canvas c){super.onDraw(c);
        long now=SystemClock.uptimeMillis();float t=motion?now/1000f:0;String s=state;boolean off=s.equals("off");
        paint.setColorFilter(off?sleepy:null);
        boolean slow=s.equals("calm")||s.equals("comfort")||s.equals("sleepy")||s.equals("sad");
        float bob=motion&&!off?(float)Math.sin(t*2*Math.PI/(slow?4f:2.6f))*(s.equals("celebrate")?3f:2f):0;
        if(s.equals("celebrate")&&motion)bob-=Math.abs((float)Math.sin(t*Math.PI*1.6))*5f;
        if(s.equals("excited")&&motion)bob-=Math.abs((float)Math.sin(t*Math.PI*2.4))*7f;
        if(s.equals("surprised"))bob-=3;
        float shake=s.equals("laughing")&&motion?(float)Math.sin(t*28)*1.4f:s.equals("angry")&&motion?(float)Math.sin(t*41)*1.1f:0;
        shownLevel+=(level-shownLevel)*0.55f;
        float tilt=s.equals("confused")?-6:s.equals("shy")?5:s.equals("sleepy")?4:s.equals("oops")?-4:0;
        boolean blink=false;if(motion&&!off){if(now>nextBlink+140)nextBlink=now+2600+(long)(Math.random()*2600);blink=now>=nextBlink;}
        float flap=motion&&!off?(float)Math.sin(t*2*Math.PI*(busy()?2.4f:1.2f))*(busy()?8:5):0;
        // Wings lift when surprised and droop when sleepy or sad.
        if(s.equals("surprised"))flap=10;else if(s.equals("sleepy")||s.equals("sad"))flap=-9+flap*0.3f;

        c.save();float k=Math.min(getWidth()/W,getHeight()/H);c.translate((getWidth()-W*k)/2,(getHeight()-H*k)/2);c.scale(k,k);c.translate(20,4);
        oval(c,-28,10,228,250,glow);
        oval(c,66,232,134,240,0x22303a6a);
        // Twinkling stars around the fairy.
        float[][] twinkles={{-8,40,5},{206,52,4},{-12,206,4},{212,200,5},{196,128,3},{4,130,3}};
        for(int i=0;i<twinkles.length;i++){float a=off?0.35f:motion?0.35f+0.65f*Math.abs((float)Math.sin(t*1.7f+i*1.3f)):1;sparkle(c,twinkles[i][0],twinkles[i][1],twinkles[i][2],((int)(a*255)<<24)|(i%2==0?0xffe6a3:0xffffff));}
        c.translate(shake,bob);c.rotate(tilt,100,232);

        // Wings flutter from the shoulders.
        for(int side=0;side<2;side++){c.save();if(side==1)c.scale(-1,1,100,0);c.rotate(flap,88,154);
            fill(c,wingLow,wing);stroke(c,wingLow,0xccffffff,1.6f);fill(c,wingTop,wing);stroke(c,wingTop,0xeeffffff,1.8f);stroke(c,veins,0x66b7b0ea,1.1f);
            stroke(c,wingTop,0x99aaa3e6,0.8f);sparkle(c,22,106,4.5f,0xffffffff);sparkle(c,4,124,3,0xeefff3c4);sparkle(c,26,190,3.5f,0xeefff3c4);c.restore();}

        fill(c,hairBack,hair);
        // Legs, boots, skirt and bodice.
        oval(c,86,204,97,224,0xfffde6dc);oval(c,103,204,114,224,0xfffde6dc);
        oval(c,78,222,100,238,0xffd3e0fa);strokeOval(c,78,222,100,238,0xff8ea6de,1.2f);oval(c,100,222,122,238,0xffd3e0fa);strokeOval(c,100,222,122,238,0xff8ea6de,1.2f);
        oval(c,80,212,100,226,0xfffcfcff);strokeOval(c,80,212,100,226,0xffe3e7f6,1);oval(c,100,212,120,226,0xfffcfcff);strokeOval(c,100,212,120,226,0xffe3e7f6,1);
        star(c,89,231,3.8f,0xfff3c34c);star(c,111,231,3.8f,0xfff3c34c);
        fill(c,underSkirt,0xfff8f9ff);stroke(c,underSkirt,0xffdde3f5,1);
        fill(c,skirt,skirtFill);stroke(c,skirt,0xff8ea9e2,1.2f);fill(c,petals,0x66ffffff);
        oval(c,60,164,75,184,0xfffde6dc);oval(c,125,164,140,184,0xfffde6dc);
        oval(c,64,148,88,170,0xfffbfcff);strokeOval(c,64,148,88,170,0xffd5dcf3,1);oval(c,112,148,136,170,0xfffbfcff);strokeOval(c,112,148,136,170,0xffd5dcf3,1);
        path.reset();path.addRoundRect(82,150,118,170,6,6,Path.Direction.CW);fill(c,path,0xffc7d9f8);
        oval(c,86,147,100,156,0xffffffff);oval(c,100,147,114,156,0xffffffff);
        fill(c,bowLoops,0xff6f94da);stroke(c,bowLoops,0xff5277c4,0.8f);star(c,100,158,4.8f,0xfff3c34c);oval(c,98,163,102,167,0xfffdfbf5);

        // Face, cheeks and eyes.
        oval(c,44,56,156,152,face);
        boolean flustered=s.equals("shy")||s.equals("oops");
        float blushA=s.equals("angry")?1.45f:flustered||s.equals("grateful")||s.equals("laughing")||s.equals("excited")?1.35f:s.equals("celebrate")||s.equals("playful")||s.equals("comfort")?1.2f:1f;
        for(float bx:new float[]{64,136}){c.save();c.translate(bx,127);c.scale(blushA*1.15f,blushA*0.62f);oval(c,-12,-12,12,12,blush);c.restore();
            if(flustered){path.reset();for(int i=0;i<3;i++){path.moveTo(bx-5+i*5,123.5f);path.lineTo(bx-8+i*5,130.5f);}stroke(c,path,0xccf27d9a,1.4f);}}
        oval(c,99,123,101.5f,125,0x44e0907e);
        String le="open",re="open";float ls=1.2f,rs=1.2f,lookX=0,lookY=0;
        switch(s){
            case "celebrate":case "calm":case "laughing":case "grateful":le=re="happy";break;
            case "playful":re="happy";break;
            case "curious":case "listening":ls=rs=1.3f;break;
            case "concerned":ls=rs=1.12f;break;
            case "thinking":lookX=2.5f;lookY=-2.5f;break;
            case "surprised":le=re="wide";ls=rs=1.36f;break;
            case "shy":lookX=-2.5f;lookY=3;break;
            case "sleepy":le=re="half";break;
            case "sad":lookY=2;ls=rs=1.22f;break;
            case "excited":le=re="star";ls=rs=1.3f;break;
            case "confused":ls=1.27f;rs=1.04f;lookX=-1.5f;lookY=-2;break;
            case "oops":le=re="squeeze";break;
            case "angry":le=re="angry";ls=rs=1.16f;break;
        }
        if(off)le=re="closed";
        else if(blink){if(!le.equals("happy")&&!le.equals("squeeze")&&!le.equals("angry"))le="closed";if(!re.equals("happy")&&!re.equals("squeeze")&&!re.equals("angry"))re="closed";}
        eye(c,72,114,-1,ls,le,lookX,lookY);
        eye(c,128,114,1,rs,re,lookX,lookY);
        drawMouth(c,talking?"talking":s,t);

        // Hair in front of the face.
        c.save();c.translate(0,4);fill(c,hairCap,0x2a8f86d0);c.restore();
        fill(c,sideLocks,hair);fill(c,hairCap,hair);stroke(c,locks,0x2e7f92d8,1.3f);
        path.reset();path.moveTo(56,54);path.quadTo(64,45,75,41);path.moveTo(86,37);path.quadTo(100,34,113,36);path.moveTo(124,39);path.quadTo(136,44,144,53);stroke(c,path,0xccffffff,3.2f);
        path.reset();path.moveTo(76,60);path.quadTo(80,70,78,80);path.moveTo(112,56);path.quadTo(116,66,114,76);stroke(c,path,0x77ffffff,2.2f);
        c.save();c.rotate(motion&&!off?(float)Math.sin(t*2.2f)*7:0,101,30);fill(c,ahoge,hair);c.restore();
        path.reset();
        if(s.equals("concerned")||s.equals("comfort")||s.equals("sad")||s.equals("oops")){path.moveTo(64,95);path.lineTo(80,91);path.moveTo(136,95);path.lineTo(120,91);}
        else if(s.equals("curious")){path.moveTo(64,90);path.quadTo(72,85,81,89);path.moveTo(119,89);path.quadTo(128,85,136,90);}
        else if(s.equals("surprised")){path.moveTo(61,87);path.quadTo(72,79,83,85);path.moveTo(117,85);path.quadTo(128,79,139,87);}
        else if(s.equals("confused")){path.moveTo(61,88);path.quadTo(72,80,83,85);path.moveTo(119,93);path.lineTo(136,95);}
        stroke(c,path,0xcc7c78c2,2.2f);
        if(s.equals("angry")){path.reset();path.moveTo(60,85);path.lineTo(82,95);path.moveTo(140,85);path.lineTo(118,95);stroke(c,path,0xff5e5aa8,3.4f);}
        clip(c);

        // Expression extras.
        if(s.equals("thinking")){for(int i=0;i<3;i++){float lift=motion?Math.max(0,(float)Math.sin(t*6-i*0.9f))*4:0;float r=3+i*1.3f;oval(c,152+i*12-r,58-i*13-lift-r,152+i*12+r,58-i*13-lift+r,0xffc5d2fa);strokeOval(c,152+i*12-r,58-i*13-lift-r,152+i*12+r,58-i*13-lift+r,0xffffffff,1.2f);}}
        if(s.equals("listening")){float reach=motion?4+level*12:4;for(int i=0;i<2;i++){path.reset();float x=170+i*7;path.moveTo(x,96-reach*0.6f-i*4);path.quadTo(x+6+reach*0.5f,112,x,128+reach*0.6f+i*4);stroke(c,path,i==0?0xff8fa3e2:0x998fa3e2,2.6f);}}
        if(s.equals("comfort"))heart(c,166,70-(motion?(float)Math.sin(t*2)*3:0),7,0xffff9db4);
        if(s.equals("concerned")){path.reset();path.moveTo(158,74);path.quadTo(152,86,158,88);path.quadTo(164,86,158,74);path.close();fill(c,path,0xffaed8f7);}
        if(s.equals("celebrate")){for(int i=0;i<4;i++){float a=(motion?t*1.8f:0)+i*1.57f;star(c,100+84*(float)Math.cos(a),110+70*(float)Math.sin(a),5,i%2==0?0xfff3c34c:0xffff9db4);}}
        if(s.equals("angry")){
            // Anime anger mark and little steam puffs.
            float pulse=motion?1+0.15f*(float)Math.abs(Math.sin(t*5)):1;c.save();c.scale(pulse,pulse,154,48);
            float x=154,y=48;path.reset();path.moveTo(x-2,y-7);path.quadTo(x-2,y-2,x-7,y-2);path.moveTo(x+2,y-7);path.quadTo(x+2,y-2,x+7,y-2);
            path.moveTo(x-2,y+7);path.quadTo(x-2,y+2,x-7,y+2);path.moveTo(x+2,y+7);path.quadTo(x+2,y+2,x+7,y+2);stroke(c,path,0xffe2475f,2.6f);c.restore();
            for(int i=0;i<2;i++){float rise=motion?(t*20+i*12)%24:8;int a=(int)(200*(1-rise/26));float px=i==0?28:172;oval(c,px-6,72-rise-5,px+6,72-rise+5,(a<<24)|0xe6e9f2);}
        }
        if(s.equals("surprised")){path.reset();path.addRoundRect(160,34,167,56,3.5f,3.5f,Path.Direction.CW);fill(c,path,0xfff3a93c);oval(c,160,59,167,66,0xfff3a93c);}
        if(s.equals("confused")){paint.setShader(null);paint.setColor(0xff8fa3e2);paint.setTextSize(30);paint.setFakeBoldText(true);c.drawText("?",154,64-(motion?(float)Math.abs(Math.sin(t*2.5))*4:0),paint);paint.setFakeBoldText(false);}
        if(s.equals("sad")){path.reset();for(float ex:new float[]{72,128}){path.moveTo(ex-12,123);path.quadTo(ex,131,ex+12,123);}stroke(c,path,0xb3c8e8ff,3.2f);float drip=motion?(t*9)%14:4;int a=(int)(255*(1-drip/16));drop(c,60,124+drip,(a<<24)|0xaed8f7);}
        if(s.equals("laughing")){drop(c,51,108,0xffaed8f7);drop(c,149,108,0xffaed8f7);}
        if(s.equals("oops"))drop(c,158,74,0xffaed8f7);
        if(s.equals("excited")){for(int i=0;i<6;i++){double a=i*Math.PI/3+0.4;float r=4+(motion?3*Math.abs((float)Math.sin(t*4+i)):2);sparkle(c,100+(float)Math.cos(a)*92,108+(float)Math.sin(a)*82,r,i%3==0?0xfff3c34c:i%3==1?0xffffffff:0xffff9db4);}}
        if(s.equals("grateful")){for(int i=0;i<3;i++){float rise=motion?(t*14+i*16)%48:i*16;int a=(int)(255*(1-rise/52));heart(c,i==1?40:162+i*4,76-rise,5+i,(a<<24)|0xff9db4);}}
        if(s.equals("sleepy")){paint.setShader(null);paint.setFakeBoldText(true);int a=motion?(int)(150+100*Math.abs(Math.sin(t*1.5))):220;paint.setColor((a<<24)|0x8fa3e2);paint.setTextSize(14);c.drawText("z",150,56,paint);paint.setTextSize(11);c.drawText("z",160,45,paint);paint.setFakeBoldText(false);}
        if(off){paint.setShader(null);paint.setColor(0xff9aa5c8);paint.setTextSize(16);paint.setFakeBoldText(true);c.drawText("z",150,52,paint);paint.setTextSize(12);c.drawText("z",162,40,paint);paint.setFakeBoldText(false);}
        c.restore();paint.setColorFilter(null);
    }

    /** A small water drop: a tear, a joyful tear or a sweat drop. */
    private void drop(Canvas c,float x,float y,int color){path.reset();path.moveTo(x,y);path.quadTo(x-5,y+9,x,y+11);path.quadTo(x+5,y+9,x,y);path.close();fill(c,path,color);}

    /** Flower, star and pearls pinned on the left of the hair. */
    private void clip(Canvas c){
        path.reset();path.moveTo(48,52);path.lineTo(54,62);path.moveTo(62,48);path.lineTo(57,61);stroke(c,path,0xffd8c9a8,1);
        oval(c,45,48,51,54,0xfffdfbf5);oval(c,59,44,65,50,0xfffdfbf5);
        path.reset();path.moveTo(44,77);path.lineTo(39,90);path.moveTo(45,77);path.lineTo(49,89);stroke(c,path,0xff7b9fe0,2.2f);
        path.reset();path.moveTo(44,77);path.quadTo(30,66,32,79);path.quadTo(35,86,44,78);path.close();path.moveTo(45,77);path.quadTo(58,67,57,80);path.quadTo(53,86,45,78);path.close();fill(c,path,0xff7b9fe0);
        oval(c,41.5f,74.5f,47.5f,80.5f,0xff5f84cf);
        for(int i=0;i<5;i++){double a=i*Math.PI*2/5-Math.PI/2;float px=54+(float)Math.cos(a)*6.5f,py=64+(float)Math.sin(a)*6.5f;oval(c,px-5,py-5,px+5,py+5,0xfffffdfb);strokeOval(c,px-5,py-5,px+5,py+5,0xfff4d6dc,0.8f);}
        oval(c,51,61,57,67,0xfff6c955);star(c,64,77,5,0xfff3c34c);
    }

    private void eye(Canvas c,float x,float y,int outer,float scale,String mode,float lx,float ly){
        c.save();c.translate(x,y);c.scale(scale,scale);
        if(mode.equals("open")||mode.equals("wide")||mode.equals("star")||mode.equals("half")||mode.equals("angry")){
            oval(c,-13,-15,13,15,iris);strokeOval(c,-13,-15,13,15,0xff131a3c,1.6f);
            if(mode.equals("star"))star(c,lx,1+ly,8.5f,0xfffff1c2);
            else if(mode.equals("wide"))oval(c,-3.6f+lx,-4.2f+ly,3.6f+lx,4.2f+ly,0xff0b1130);
            else oval(c,-6+lx,-7+ly,6+lx,7+ly,0xff0b1130);
            path.reset();path.moveTo(-9,8);path.quadTo(0,15,9,8);stroke(c,path,0x8899c6ff,2.2f);
            oval(c,-9+lx,-12+ly,-1+lx,-3+ly,0xffffffff);if(!mode.equals("star"))sparkle(c,5.5f+lx,-6+ly,4.6f,0xffffffff);oval(c,4.5f,5.5f,7.5f,8.5f,0xeeffffff);
            float o=outer;path.reset();path.moveTo(-o*12,-8);path.quadTo(-o*2,-19.5f,o*10,-15.5f);path.quadTo(o*15.5f,-13.5f,o*17.5f,-5.5f);
            path.quadTo(o*13,-10,o*9,-12.2f);path.quadTo(-o*2,-15.5f,-o*12,-8);path.close();fill(c,path,0xff151a33);
            path.reset();path.moveTo(o*14.5f,-11);path.lineTo(o*20,-15.5f);path.moveTo(o*16.5f,-7.5f);path.lineTo(o*21.5f,-9);stroke(c,path,0xff151a33,1.6f);
            if(mode.equals("half")){
                // Drowsy eyelid: skin over the upper eye only, a soft drooping lid line and small lashes.
                c.save();path.reset();path.addOval(-15,-17.5f,15,17.5f,Path.Direction.CW);c.clipPath(path);
                path.reset();path.moveTo(-18,-22);path.lineTo(18,-22);path.lineTo(18,1);path.quadTo(0,8,-18,1);path.close();fill(c,path,0xfffde9e0);c.restore();
                path.reset();path.moveTo(-14.5f,0.5f);path.quadTo(0,8,14.5f,0.5f);stroke(c,path,0xff151a33,2.8f);
                path.reset();path.moveTo(o*12.5f,2.5f);path.lineTo(o*16,6);path.moveTo(o*7,5);path.lineTo(o*8.5f,8.5f);stroke(c,path,0xff151a33,1.4f);
            }
            if(mode.equals("angry")){
                // Angry lids slope down toward the nose: skin above the slant, then a firm lid line.
                c.save();path.reset();path.addOval(-15,-17.5f,15,17.5f,Path.Direction.CW);c.clipPath(path);
                path.reset();path.moveTo(o*16,-14);path.lineTo(-o*16,-3);path.lineTo(-o*16,-24);path.lineTo(o*16,-24);path.close();fill(c,path,0xfffde9e0);c.restore();
                path.reset();path.moveTo(o*15.5f,-13.5f);path.lineTo(-o*14.5f,-4);stroke(c,path,0xff151a33,3.2f);
            }
        }else if(mode.equals("squeeze")){
            float o=outer;path.reset();path.moveTo(o*11,-9);path.lineTo(-o*7,0);path.lineTo(o*11,9);stroke(c,path,0xff151a33,3.2f);
        }else if(mode.equals("happy")){
            path.reset();path.moveTo(-13,4);path.quadTo(0,-12,13,4);stroke(c,path,0xff151a33,3.2f);
        }else{
            path.reset();path.moveTo(-14,0);path.quadTo(0,8,14,0);stroke(c,path,0xff151a33,3f);
            path.reset();path.moveTo(-8,4);path.lineTo(-10,8);path.moveTo(0,5.5f);path.lineTo(0,9.5f);path.moveTo(8,4);path.lineTo(10,8);stroke(c,path,0xff151a33,1.4f);
        }
        c.restore();
    }

    private void drawMouth(Canvas c,String s,float t){
        mouth.reset();
        switch(s){
            case "talking":{float open=motion?2.5f+11*shownLevel:7;mouth.addOval(94.5f,131,105.5f,131+open,Path.Direction.CW);openMouth(c,false);return;}
            case "angry":{mouth.addRoundRect(92.5f,131.5f,107.5f,140.5f,3.5f,3.5f,Path.Direction.CW);fill(c,mouth,0xff7a2e3e);
                path.reset();path.addRect(93.5f,132.5f,106.5f,136,Path.Direction.CW);fill(c,path,0xffffffff);
                path.reset();path.moveTo(97,132.5f);path.lineTo(97,136);path.moveTo(100,132.5f);path.lineTo(100,136);path.moveTo(103,132.5f);path.lineTo(103,136);stroke(c,path,0x55a05060,0.8f);return;}
            case "speaking":{float open=motion?3+8*Math.abs((float)Math.sin(t*11)*(0.6f+0.4f*(float)Math.sin(t*3.7))):7;mouth.addOval(94,131,106,131+open,Path.Direction.CW);openMouth(c,false);return;}
            case "curious":mouth.addOval(96,131,104,140,Path.Direction.CW);openMouth(c,false);return;
            case "surprised":mouth.addOval(94.5f,130,105.5f,144,Path.Direction.CW);openMouth(c,false);return;
            case "sleepy":{float yawn=motion?3*Math.abs((float)Math.sin(t*1.2f)):2;mouth.addOval(95.5f,131,104.5f,138+yawn,Path.Direction.CW);openMouth(c,false);return;}
            case "shy":path.reset();path.moveTo(95,134);path.quadTo(97.5f,136.5f,100,134);path.quadTo(102.5f,136.5f,105,134);stroke(c,path,0xff9a4a5a,2.2f);return;
            case "sad":path.reset();path.moveTo(94,138);path.quadTo(100,132,106,138);stroke(c,path,0xff9a4a5a,2.2f);return;
            case "confused":path.reset();path.moveTo(95,137);path.quadTo(98.5f,133.5f,101,136);path.quadTo(103.5f,138,106,134.5f);stroke(c,path,0xff9a4a5a,2.2f);return;
            case "oops":path.reset();path.moveTo(91,135);path.quadTo(94,132,97,135);path.quadTo(100,138,103,135);path.quadTo(106,132,109,135);stroke(c,path,0xff9a4a5a,2.2f);return;
            case "comfort":case "calm":case "listening":case "grateful":path.reset();path.moveTo(93,133);path.quadTo(100,139,107,133);stroke(c,path,0xff9a4a5a,2.2f);return;
            case "concerned":path.reset();path.moveTo(93,136);path.quadTo(96.5f,133,100,136);path.quadTo(103.5f,139,107,136);stroke(c,path,0xff9a4a5a,2.2f);return;
            case "thinking":path.reset();path.moveTo(96,136);path.quadTo(101,133,106,134);stroke(c,path,0xff9a4a5a,2.2f);return;
            case "off":path.reset();path.moveTo(96,135);path.lineTo(104,135);stroke(c,path,0xff9a4a5a,2);return;
            default:{float big=s.equals("laughing")?1.45f:s.equals("excited")?1.3f:s.equals("celebrate")?1.25f:1;c.save();c.scale(big,big,100,131);
                mouth.moveTo(91,131);mouth.quadTo(100,133,109,131);mouth.quadTo(107,144,100,144);mouth.quadTo(93,144,91,131);mouth.close();
                openMouth(c,true);c.restore();
                if(s.equals("playful")){oval(c,96.5f,140,104.5f,149,0xfff58ea3);path.reset();path.moveTo(100.5f,142);path.lineTo(100.5f,147);stroke(c,path,0xffd86a82,1);}
            }
        }
    }
    /** Fills the current mouth shape with a tongue inside and, for a smile, a tiny fang. */
    private void openMouth(Canvas c,boolean fang){
        fill(c,mouth,0xff7a2e3e);c.save();c.clipPath(mouth);oval(c,93,138,107,148,0xfff58ea3);c.restore();
        if(fang){path.reset();path.moveTo(94,131.4f);path.lineTo(95.8f,135);path.lineTo(97.6f,131.6f);path.close();fill(c,path,0xffffffff);}
    }
}
