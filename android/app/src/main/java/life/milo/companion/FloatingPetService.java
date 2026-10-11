package life.milo.companion;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.graphics.PixelFormat;
import android.os.*;
import android.provider.Settings;
import android.view.*;

/** User-started visible pet overlay, with no microphone, camera or screen-reading access. */
public final class FloatingPetService extends Service {
    static volatile boolean visible;
    WindowManager windows;WindowManager.LayoutParams layout;PetFaceView face;
    SharedPreferences prefs;Handler ui=new Handler(Looper.getMainLooper());
    float downX,downY;int startX,startY;long downAt;boolean dragged;
    final Runnable refresh=new Runnable(){public void run(){if(face==null)return;if(!prefs.getBoolean("pet_enabled",true)){stopSelf();return;}face.setExpression(prefs.getString("pet_expression",CompanionStyle.forMood(prefs.getString("mood","Okay"))));ui.postDelayed(this,750);}};
    @Override public IBinder onBind(Intent intent){return null;}
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(intent==null||"STOP".equals(intent.getAction())){stopSelf();return START_NOT_STICKY;}
        if(face!=null)return START_NOT_STICKY;
        prefs=getSharedPreferences("milo",MODE_PRIVATE);
        if(!prefs.getBoolean("pet_enabled",true)||!Settings.canDrawOverlays(this)){stopSelf();return START_NOT_STICKY;}
        NotificationManager manager=getSystemService(NotificationManager.class);manager.createNotificationChannel(new NotificationChannel("floating_pet","Floating Milo pet",NotificationManager.IMPORTANCE_LOW));
        PendingIntent open=PendingIntent.getActivity(this,71,new Intent(this,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        PendingIntent stop=PendingIntent.getService(this,72,new Intent(this,FloatingPetService.class).setAction("STOP"),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification notice=new Notification.Builder(this,"floating_pet").setSmallIcon(R.drawable.milo_icon).setContentTitle(prefs.getString("pet_name","Milo")+" pet is visible").setContentText("Drag to move · tap to open · Hide pet to stop").setContentIntent(open).setOngoing(true).setVisibility(Notification.VISIBILITY_PRIVATE).addAction(new Notification.Action.Builder(null,"Hide pet",stop).build()).build();
        try{
            if(Build.VERSION.SDK_INT>=34)startForeground(502,notice,ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);else startForeground(502,notice);
            windows=getSystemService(WindowManager.class);face=new PetFaceView(this);face.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);face.setContentDescription("Floating "+prefs.getString("pet_name","Milo")+" pet. Tap to open. Drag to move. Long press to hide.");face.setOnClickListener(v->startActivity(new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP)));
            layout=new WindowManager.LayoutParams(dp(86),dp(76),WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,PixelFormat.TRANSLUCENT);layout.gravity=Gravity.TOP|Gravity.LEFT;layout.x=prefs.getInt("pet_x",24);layout.y=prefs.getInt("pet_y",180);clamp();
            face.setOnTouchListener((v,event)->{switch(event.getActionMasked()){
                case MotionEvent.ACTION_DOWN:downX=event.getRawX();downY=event.getRawY();startX=layout.x;startY=layout.y;downAt=System.currentTimeMillis();dragged=false;return true;
                case MotionEvent.ACTION_MOVE:float dx=event.getRawX()-downX,dy=event.getRawY()-downY;if(Math.abs(dx)+Math.abs(dy)>dp(8))dragged=true;layout.x=startX+(int)dx;layout.y=startY+(int)dy;clamp();try{windows.updateViewLayout(face,layout);}catch(Exception ignored){stopSelf();}return true;
                case MotionEvent.ACTION_UP:if(dragged)prefs.edit().putInt("pet_x",layout.x).putInt("pet_y",layout.y).apply();else if(System.currentTimeMillis()-downAt>600)stopSelf();else v.performClick();return true;
                case MotionEvent.ACTION_CANCEL:return true;
                default:return false;
            }});
            windows.addView(face,layout);visible=true;ui.post(refresh);
        }catch(Exception e){stopSelf();android.widget.Toast.makeText(this,"Android could not show the floating pet. Check display-over-other-apps permission.",android.widget.Toast.LENGTH_LONG).show();}
        return START_NOT_STICKY;
    }
    int dp(int value){return (int)(value*getResources().getDisplayMetrics().density);}
    void clamp(){android.util.DisplayMetrics metrics=new android.util.DisplayMetrics();windows.getDefaultDisplay().getRealMetrics(metrics);layout.x=Math.max(0,Math.min(layout.x,metrics.widthPixels-layout.width));layout.y=Math.max(dp(30),Math.min(layout.y,metrics.heightPixels-layout.height-dp(40)));}
    @Override public void onDestroy(){visible=false;ui.removeCallbacksAndMessages(null);if(face!=null&&windows!=null)try{windows.removeView(face);}catch(Exception ignored){}face=null;stopForeground(STOP_FOREGROUND_REMOVE);super.onDestroy();}
}
