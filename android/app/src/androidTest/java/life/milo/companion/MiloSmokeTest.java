package life.milo.companion;

import android.test.ActivityInstrumentationTestCase2;
import java.util.concurrent.atomic.AtomicReference;

/** Device tests: offline startup, screens, encrypted personal key, and mood persistence. */
@SuppressWarnings("deprecation")
public class MiloSmokeTest extends ActivityInstrumentationTestCase2<MainActivity> {
    public MiloSmokeTest(){super(MainActivity.class);}
    /* Tests run on the owner's real phone: put every Milo setting (chosen AI provider, models, switches) back afterwards. */
    java.util.Map<String,?> savedPrefs;
    @Override protected void setUp() throws Exception {super.setUp();savedPrefs=new java.util.HashMap<>(getInstrumentation().getTargetContext().getSharedPreferences("milo",0).getAll());}
    @SuppressWarnings("unchecked")
    @Override protected void tearDown() throws Exception {
        java.util.Map<String,?> saved=savedPrefs;android.content.Context c=getInstrumentation().getTargetContext();
        super.tearDown(); // also nulls this test's fields
        android.content.SharedPreferences.Editor e=c.getSharedPreferences("milo",0).edit().clear();
        for(java.util.Map.Entry<String,?> p:saved.entrySet()){Object v=p.getValue();String k=p.getKey();
            if(v instanceof String)e.putString(k,(String)v);else if(v instanceof Boolean)e.putBoolean(k,(Boolean)v);else if(v instanceof Integer)e.putInt(k,(Integer)v);
            else if(v instanceof Long)e.putLong(k,(Long)v);else if(v instanceof Float)e.putFloat(k,(Float)v);else if(v instanceof java.util.Set)e.putStringSet(k,(java.util.Set<String>)v);}
        e.commit();
    }
    public void testSmallLocalModelPromptUsesQwenChatFormat() throws Exception {
        String prompt=PortableLocalAi.localPrompt(true,"Milo","English","","Gentle","Hello",new java.util.ArrayList<>(),"Mood: Okay");
        assertTrue(prompt.startsWith("<|im_start|>system\n"));
        assertTrue(prompt.contains("<|im_start|>user\nHello<|im_end|>"));
        assertTrue(prompt.endsWith("<|im_start|>assistant\n"));
        assertFalse(prompt.contains("<start_of_turn>"));
    }
    /** Runs only where a model was imported (e.g. the owner's phone); emulators skip it. */
    public void testImportedLocalModelStreamsAndStaysLoaded() throws Exception {
        android.content.Context c=getInstrumentation().getTargetContext();
        if(!PortableLocalAi.ready(c)){android.util.Log.i("MiloLocalAi","test skipped: no imported model");return;}
        PortableLocalAi.release();assertNull(PortableLocalAi.engine);
        PortableLocalAi ai=new PortableLocalAi(c);java.util.List<String> parts=java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        long t0=android.os.SystemClock.elapsedRealtime();
        MainActivity.GeminiReply first=ai.chat("","Milo","English","","Gentle","Say hello in two short sentences.",new java.util.ArrayList<>(),"Current mood: Okay.",false,parts::add);
        long cold=android.os.SystemClock.elapsedRealtime()-t0;
        assertFalse(first.text.isEmpty());
        assertTrue("reply should arrive in several pieces, got "+parts.size(),parts.size()>1);
        for(int i=1;i<parts.size();i++)assertTrue("each piece extends the previous text",parts.get(i).startsWith(parts.get(i-1)));
        assertEquals(first.text,parts.get(parts.size()-1).trim());
        Object loaded=PortableLocalAi.engine;assertNotNull(loaded);
        t0=android.os.SystemClock.elapsedRealtime();
        MainActivity.GeminiReply second=ai.chat("","Milo","English","","Gentle","Name one small goal for today.",new java.util.ArrayList<>(),"Current mood: Okay.",false,p->{});
        long warm=android.os.SystemClock.elapsedRealtime()-t0;
        assertFalse(second.text.isEmpty());assertSame(loaded,PortableLocalAi.engine);
        android.util.Log.i("MiloLocalAi","test cold="+cold+"ms warm="+warm+"ms pieces="+parts.size());
        PortableLocalAi.release();assertNull(PortableLocalAi.engine);
    }
    /** Runs only where Gemma 4 is downloaded: streaming, tone tag, live conversation reuse and Malayalam replies. */
    public void testGemma4StreamsKeepsConversationAndRepliesInMalayalam() throws Exception {
        android.content.Context c=getInstrumentation().getTargetContext();
        if(!LiteRtLocalAi.ready(c)){android.util.Log.i("MiloLocalAi","Gemma 4 test skipped: model not downloaded");return;}
        LiteRtLocalAi.release();LiteRtLocalAi ai=new LiteRtLocalAi(c);java.util.List<String> parts=java.util.Collections.synchronizedList(new java.util.ArrayList<>());java.util.List<org.json.JSONObject> history=new java.util.ArrayList<>();
        long t0=android.os.SystemClock.elapsedRealtime();
        MainActivity.GeminiReply first=ai.chat("","Milo","English","","Gentle","Hi Milo! I finished my first Python lesson today.",history,"Current mood: Good.",false,parts::add);
        long cold=android.os.SystemClock.elapsedRealtime()-t0;
        assertFalse(first.text.isEmpty());assertFalse("tone tag must be hidden: "+first.text,first.text.startsWith("["));
        assertTrue("reply should stream in pieces, got "+parts.size(),parts.size()>1);
        for(int i=1;i<parts.size();i++)assertTrue("streamed text grows in order",parts.get(i).startsWith(parts.get(i-1)));
        history.add(MainActivity.turn("user","Hi Milo! I finished my first Python lesson today."));history.add(MainActivity.turn("model",first.text));
        t0=android.os.SystemClock.elapsedRealtime();
        MainActivity.GeminiReply second=ai.chat("","Milo","English","","Gentle","What should I learn next?",history,"Current mood: Good.",false,p->{});
        long followUp=android.os.SystemClock.elapsedRealtime()-t0;
        assertFalse(second.text.isEmpty());assertEquals("follow-up reuses the live conversation",4,LiteRtLocalAi.conversationTurns.size());
        t0=android.os.SystemClock.elapsedRealtime();
        MainActivity.GeminiReply ml=ai.chat("","Milo","Malayalam","","Gentle","ഇന്ന് എനിക്ക് കുറച്ച് ക്ഷീണമുണ്ട്. എന്ത് ചെയ്യാം?",new java.util.ArrayList<>(),"Current mood: Tired.",false,p->{});
        long malayalam=android.os.SystemClock.elapsedRealtime()-t0;
        int script=0,letters=0;for(char ch:ml.text.toCharArray()){if(Character.isLetter(ch)){letters++;if(ch>='ഀ'&&ch<='ൿ')script++;}}
        assertTrue("Malayalam reply should use Malayalam script: "+ml.text,letters>0&&script*10>=letters*7);
        android.util.Log.i("MiloLocalAi","Gemma4 test backend="+LiteRtLocalAi.backend()+" cold="+cold+"ms followUp="+followUp+"ms malayalam="+malayalam+"ms pieces="+parts.size()+" tones="+first.tone+","+second.tone+","+ml.tone);
        // Stories and pep talks must be full length, not a quick one-liner.
        MainActivity.GeminiReply story=ai.chat("","Milo","English","","Gentle","Please tell me a bedtime story about a brave little star.",new java.util.ArrayList<>(),"Current mood: Okay.",false,x->{});
        MainActivity.GeminiReply pep=ai.chat("","Milo","English","","Gentle","Motivate me, I feel too lazy to study today.",new java.util.ArrayList<>(),"Current mood: Tired.",false,x->{});
        int storyWords=story.text.split("\s+").length,pepWords=pep.text.split("\s+").length;
        android.util.Log.i("MiloLocalAi","story words="+storyWords+" pep words="+pepWords);android.util.Log.i("MiloLocalAi","STORY: "+story.text);
        assertTrue("a story should be long, got "+storyWords+" words",storyWords>=120);assertTrue("a pep talk should be full, got "+pepWords+" words",pepWords>=60);
        android.util.Log.i("MiloLocalAi","EN1: "+first.text);android.util.Log.i("MiloLocalAi","EN2: "+second.text);android.util.Log.i("MiloLocalAi","ML: "+ml.text);
        LiteRtLocalAi.release();assertNull(LiteRtLocalAi.engine);
    }
    /** Floating pet end to end with Gemma 4: a Malayalam sentence in, spoken Malayalam reply out, with live expressions. */
    public void testFloatingPetTalksInMalayalamWithExpressions() throws Throwable {
        MainActivity a=getActivity();
        if(!android.provider.Settings.canDrawOverlays(a)||!LiteRtLocalAi.ready(a)){android.util.Log.i("MiloPet","pet talk test skipped: needs overlay permission and Gemma 4");return;}
        runTestOnUiThread(()->{a.prefs.edit().putBoolean("pet_enabled",true).putBoolean("portable_local",true).putString("portable_model",LiteRtLocalAi.KIND).putString("ai_provider",AiProvider.LOCAL).putString("voice_language","ml-IN").commit();a.launchFloatingPet();});
        long end=android.os.SystemClock.uptimeMillis()+10000;
        while(FloatingPetService.current==null&&android.os.SystemClock.uptimeMillis()<end)android.os.SystemClock.sleep(100);
        FloatingPetService pet=FloatingPetService.current;assertNotNull("pet did not start",pet);
        end=android.os.SystemClock.uptimeMillis()+10000;while(!pet.ttsReady&&android.os.SystemClock.uptimeMillis()<end)android.os.SystemClock.sleep(100);
        assertTrue("a Malayalam voice should be ready",pet.ttsReady);
        runTestOnUiThread(()->a.moveTaskToBack(true));
        // Same preparation the pet does while the user is speaking.
        LiteRtLocalAi.prepare(a,pet.name(),pet.language(),a.prefs.getString("practice_language",""),a.prefs.getString("companion_style","Gentle"),pet.voiceContext(),new java.util.ArrayList<>(pet.history));
        android.os.SystemClock.sleep(2000); // the user is still talking
        java.util.LinkedHashSet<String> seen=new java.util.LinkedHashSet<>();long start=android.os.SystemClock.uptimeMillis(),firstSpeech=-1;boolean shot=false;
        a.startForegroundService(new android.content.Intent(a,FloatingPetService.class).setAction("SAY").putExtra("text","നമസ്കാരം മിലോ! ഇന്ന് ചെയ്യാൻ ഒരു ചെറിയ ലക്ഷ്യം പറഞ്ഞുതരാമോ?"));
        end=start+60000;
        while(android.os.SystemClock.uptimeMillis()<end){
            String e=pet.face==null?"":pet.face.expression();seen.add(e);
            if(pet.face!=null&&pet.face.talking()&&firstSpeech<0)firstSpeech=android.os.SystemClock.uptimeMillis()-start;
            if(pet.face!=null&&pet.face.talking()&&!shot&&android.os.SystemClock.uptimeMillis()-start>firstSpeech+600){shot=true;android.graphics.Bitmap b=getInstrumentation().getUiAutomation().takeScreenshot();try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(a.getExternalFilesDir(null),"milo-pet-speaking.png"))){b.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}finally{b.recycle();}}
            if(pet.history.size()==2&&(pet.mode==FloatingPetService.Mode.LISTENING||pet.mode==FloatingPetService.Mode.IDLE))break;
            android.os.SystemClock.sleep(40);
        }
        a.startService(new android.content.Intent(a,FloatingPetService.class).setAction("STOP_TALK"));
        assertEquals("one exchange recorded",2,pet.history.size());
        String reply=pet.history.get(1).getJSONArray("parts").getJSONObject(0).getString("text");
        int script=0,letters=0;for(char ch:reply.toCharArray()){if(Character.isLetter(ch)){letters++;if(ch>='ഀ'&&ch<='ൿ')script++;}}
        android.util.Log.i("MiloPet","reply="+reply+" tone="+pet.tone+" expressions="+seen+" firstSpeech="+firstSpeech+"ms");
        assertTrue("Malayalam reply expected: "+reply,letters>0&&script*10>=letters*7);
        assertTrue("pet should think, then speak: "+seen+" firstSpeech="+firstSpeech,seen.contains("thinking")&&firstSpeech>=0);
    }
    /**
     * Real voice loop: Milo in the background, a real tap on the pet, a spoken Malayalam question played through the
     * speaker, Google speech recognition, Gemma 4, and a spoken Malayalam answer. Needs internet for Malayalam listening.
     */
    public void testPetHearsSpokenMalayalamAndAnswers() throws Throwable {
        MainActivity a=getActivity();
        if(!android.provider.Settings.canDrawOverlays(a)||!LiteRtLocalAi.ready(a)||a.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)!=android.content.pm.PackageManager.PERMISSION_GRANTED){android.util.Log.i("MiloPet","voice loop test skipped");return;}
        runTestOnUiThread(()->{a.prefs.edit().putBoolean("pet_enabled",true).putBoolean("portable_local",true).putString("portable_model",LiteRtLocalAi.KIND).putString("ai_provider",AiProvider.LOCAL).putString("voice_language","ml-IN").commit();a.launchFloatingPet();});
        // The question, spoken by the phone's own Malayalam voice into a file.
        java.io.File question=new java.io.File(a.getExternalFilesDir(null),"milo-question.wav");java.util.concurrent.CountDownLatch made=new java.util.concurrent.CountDownLatch(1),ready=new java.util.concurrent.CountDownLatch(1);
        android.speech.tts.TextToSpeech[] tts={null};getInstrumentation().runOnMainSync(()->tts[0]=new android.speech.tts.TextToSpeech(a,s->ready.countDown(),MiloVoice.GOOGLE));
        assertTrue(ready.await(10,java.util.concurrent.TimeUnit.SECONDS));tts[0].setLanguage(new java.util.Locale("ml","IN"));
        tts[0].setOnUtteranceProgressListener(new android.speech.tts.UtteranceProgressListener(){public void onStart(String id){}public void onDone(String id){made.countDown();}public void onError(String id){made.countDown();}});
        tts[0].synthesizeToFile("ഒരു ചെറിയ നല്ല ശീലം പറഞ്ഞുതരാമോ",null,question,"q");assertTrue(made.await(20,java.util.concurrent.TimeUnit.SECONDS));tts[0].shutdown();
        assertTrue("question audio was not made",question.length()>10_000);
        long end=android.os.SystemClock.uptimeMillis()+10000;while(FloatingPetService.current==null&&android.os.SystemClock.uptimeMillis()<end)android.os.SystemClock.sleep(100);
        FloatingPetService pet=FloatingPetService.current;assertNotNull(pet);
        boolean background=!"false".equals(((android.test.InstrumentationTestRunner)getInstrumentation()).getArguments().getString("background"));
        if(background)runTestOnUiThread(()->a.moveTaskToBack(true));android.os.SystemClock.sleep(1500);
        // A real tap on the floating pet.
        int[] at=new int[2];pet.face.getLocationOnScreen(at);float x=at[0]+pet.face.getWidth()/2f,y=at[1]+pet.face.getHeight()/2f;long down=android.os.SystemClock.uptimeMillis();
        android.app.UiAutomation ui=getInstrumentation().getUiAutomation();
        android.view.MotionEvent e=android.view.MotionEvent.obtain(down,down,android.view.MotionEvent.ACTION_DOWN,x,y,0);e.setSource(android.view.InputDevice.SOURCE_TOUCHSCREEN);ui.injectInputEvent(e,true);e.recycle();
        e=android.view.MotionEvent.obtain(down,down+80,android.view.MotionEvent.ACTION_UP,x,y,0);e.setSource(android.view.InputDevice.SOURCE_TOUCHSCREEN);ui.injectInputEvent(e,true);e.recycle();
        end=android.os.SystemClock.uptimeMillis()+5000;while(pet.mode!=FloatingPetService.Mode.LISTENING&&android.os.SystemClock.uptimeMillis()<end)android.os.SystemClock.sleep(50);
        assertEquals("tap should start listening",FloatingPetService.Mode.LISTENING,pet.mode);
        android.os.SystemClock.sleep(700);
        android.media.AudioManager audio=a.getSystemService(android.media.AudioManager.class);int volume=audio.getStreamVolume(android.media.AudioManager.STREAM_MUSIC);
        audio.setStreamVolume(android.media.AudioManager.STREAM_MUSIC,Math.max(volume,audio.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)*2/3),0);
        android.media.MediaPlayer player=new android.media.MediaPlayer();player.setAudioAttributes(new android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_MEDIA).build());player.setDataSource(question.getPath());player.prepare();player.start();
        long played=android.os.SystemClock.uptimeMillis();while(player.isPlaying())android.os.SystemClock.sleep(20);long spoke=android.os.SystemClock.uptimeMillis();player.release();audio.setStreamVolume(android.media.AudioManager.STREAM_MUSIC,volume,0);
        long firstSpeech=-1;java.util.LinkedHashSet<String> seen=new java.util.LinkedHashSet<>();end=spoke+45000;
        while(android.os.SystemClock.uptimeMillis()<end){String ex=pet.face==null?"":pet.face.expression();seen.add(ex);if(pet.face!=null&&pet.face.talking()&&firstSpeech<0)firstSpeech=android.os.SystemClock.uptimeMillis()-spoke;if(pet.history.size()>=2&&pet.mode!=FloatingPetService.Mode.THINKING&&pet.mode!=FloatingPetService.Mode.SPEAKING)break;android.os.SystemClock.sleep(40);}
        a.startService(new android.content.Intent(a,FloatingPetService.class).setAction("STOP_TALK"));
        android.util.Log.i("MiloPet","voice loop (background="+background+"): history="+pet.history+" expressions="+seen+" questionAudio="+(spoke-played)+"ms speechAfterQuestion="+firstSpeech+"ms");
        assertTrue("pet should hear the question and answer: "+seen,pet.history.size()>=2);
        String heard=pet.history.get(0).getJSONArray("parts").getJSONObject(0).getString("text");
        assertTrue("heard Malayalam: "+heard,heard.matches(".*[ഀ-ൿ].*"));
    }
    /** Draws every expression of the fairy artwork and saves a gallery (files/milo-pet-gallery.png) for review. */
    public void testPetArtworkDrawsEveryExpression() throws Throwable {
        android.content.Context c=getInstrumentation().getTargetContext();
        String[] states={"happy","playful","curious","comfort","calm","celebrate","concerned","listening","thinking","speaking","surprised","excited","laughing","shy","grateful","sad","confused","sleepy","oops","angry","off"};
        int w=400,h=464,cols=4,rows=(states.length+cols-1)/cols;
        android.graphics.Bitmap gallery=android.graphics.Bitmap.createBitmap(w*cols,(h+40)*rows,android.graphics.Bitmap.Config.ARGB_8888);
        android.graphics.Canvas g=new android.graphics.Canvas(gallery);g.drawColor(0xfff6f8f5);
        android.graphics.Paint label=new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);label.setTextSize(30);label.setColor(0xff243830);
        int[] previous=null;
        for(int i=0;i<states.length;i++){
            String state=states[i];PetFaceView[] v={null};
            getInstrumentation().runOnMainSync(()->{v[0]=new PetFaceView(c);v[0].setExpression(state);v[0].setLevel(0.7f);v[0].measure(android.view.View.MeasureSpec.makeMeasureSpec(w,android.view.View.MeasureSpec.EXACTLY),android.view.View.MeasureSpec.makeMeasureSpec(h,android.view.View.MeasureSpec.EXACTLY));v[0].layout(0,0,w,h);});
            android.graphics.Bitmap one=android.graphics.Bitmap.createBitmap(w,h,android.graphics.Bitmap.Config.ARGB_8888);
            getInstrumentation().runOnMainSync(()->v[0].draw(new android.graphics.Canvas(one)));
            int[] px=new int[w*h];one.getPixels(px,0,w,0,0,w,h);int drawn=0;for(int p:px)if((p>>>24)>40)drawn++;
            assertTrue(state+" should draw the fairy",drawn>w*h/5);
            if(previous!=null)assertFalse(state+" should look different from the previous expression",java.util.Arrays.equals(px,previous));previous=px;
            int x=(i%cols)*w,y=(i/cols)*(h+40);g.drawBitmap(one,x,y,null);g.drawText(state,x+16,y+h+30,label);one.recycle();
        }
        try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(c.getExternalFilesDir(null),"milo-pet-gallery.png"))){gallery.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}finally{gallery.recycle();}
    }
    /** The launcher icon is an adaptive fairy icon with a themed layer; saves files/milo-icon.png for review. */
    public void testAppIconDrawsFairy() throws Throwable {
        android.content.Context c=getInstrumentation().getTargetContext();
        android.graphics.drawable.Drawable launcher=c.getDrawable(R.mipmap.ic_launcher);
        assertTrue("launcher icon should be adaptive",launcher instanceof android.graphics.drawable.AdaptiveIconDrawable);
        android.graphics.drawable.AdaptiveIconDrawable icon=(android.graphics.drawable.AdaptiveIconDrawable)launcher;
        assertNotNull("themed icon layer",icon.getMonochrome());
        int s=432;android.graphics.Bitmap sheet=android.graphics.Bitmap.createBitmap(s*4,s,android.graphics.Bitmap.Config.ARGB_8888);android.graphics.Canvas g=new android.graphics.Canvas(sheet);
        android.graphics.Paint bg=new android.graphics.Paint();
        int[] backs={0xfff6f8f5,0xff1d2430,0xffe9e7f5,0xff1d2430};
        for(int i=0;i<4;i++){bg.setColor(backs[i]);g.drawRect(i*s,0,(i+1)*s,s,bg);}
        icon.setBounds(24,24,s-24,s-24);icon.draw(g);
        g.save();g.translate(s,0);icon.setBounds(24,24,s-24,s-24);icon.draw(g);g.restore();
        android.graphics.drawable.Drawable mono=icon.getMonochrome().mutate();mono.setTint(0xff4b4a7a);g.save();g.translate(2*s,0);mono.setBounds(-48,-48,s+48,s+48);mono.draw(g);g.restore();
        android.graphics.drawable.Drawable stat=c.getDrawable(R.drawable.ic_stat_milo);stat.setBounds(3*s+96,96,4*s-96,s-96);stat.draw(g);
        int[] px=new int[s*s];sheet.getPixels(px,0,s,0,0,s,s);int colourful=0;for(int p:px){int r=(p>>16)&255,gg=(p>>8)&255,b=p&255;if(Math.max(r,Math.max(gg,b))-Math.min(r,Math.min(gg,b))>40)colourful++;}
        assertTrue("icon should show the colourful fairy, got "+colourful,colourful>s*s/10);
        try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(c.getExternalFilesDir(null),"milo-icon.png"))){sheet.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}finally{sheet.recycle();}
    }
    /** Voice Studio: characters, emotions, phone voices and on-phone AI voices render and play; saves files/voice-samples/*.wav. */
    public void testVoiceStudioCharactersEmotionsAndAiVoices() throws Throwable {
        android.content.Context c=getInstrumentation().getTargetContext();android.content.SharedPreferences p=c.getSharedPreferences("milo",0);
        java.io.File out=new java.io.File(c.getExternalFilesDir(null),"voice-samples");out.mkdirs();
        int[] starts={0},dones={0};float[] maxLevel={0};java.util.concurrent.CountDownLatch ready=new java.util.concurrent.CountDownLatch(1),played=new java.util.concurrent.CountDownLatch(1);
        MiloSpeaker[] sp={null};
        p.edit().putString("voice_language","en-US").putString("voice_name_en","").commit();
        getInstrumentation().runOnMainSync(()->sp[0]=new MiloSpeaker(c,p,new MiloSpeaker.Listener(){
            public void onStart(String id,String tone){starts[0]++;}public void onDone(String id){dones[0]++;if(id.equals("probe-final"))played.countDown();}public void onLevel(float l){maxLevel[0]=Math.max(maxLevel[0],l);}},ready::countDown));
        assertTrue("speaker did not start",ready.await(20,java.util.concurrent.TimeUnit.SECONDS));
        // {file, language, character, voice, tone, text}
        String[][] cases={
            {"en-sweet-girl-ai-happy","en-US","sweet_girl","ai:kokoro:af_heart","happy","Hi! I'm Milo. Let's have a lovely day together!"},
            {"en-sweet-girl-phone-happy","en-US","sweet_girl","phone:","happy","Hi! I'm Milo. Let's have a lovely day together!"},
            {"en-cartoon-fairy-ai-excited","en-US","cartoon_fairy","ai:kokoro:af_bella","excited","Guess what? That's amazing news!"},
            {"en-tiny-pixie-ai-playful","en-US","tiny_pixie","ai:kokoro:af_nicole","playful","Catch me if you can!"},
            {"en-sweet-girl-ai-angry","en-US","sweet_girl","ai:kokoro:af_heart","angry","That is so unfair!"},
            {"en-sweet-girl-ai-sad","en-US","sweet_girl","ai:kokoro:af_heart","sad","I'm sorry. That sounds really hard."},
            {"en-sweet-girl-ai-sleepy","en-US","sweet_girl","ai:kokoro:af_heart","sleepy","I'm getting sleepy. Good night."},
            {"en-robot-buddy-ai-happy","en-US","robot_buddy","ai:kokoro:am_michael","happy","Beep boop! Robot Milo is ready to help."},
            {"ml-sweet-girl-ai-happy","ml-IN","sweet_girl","","happy","നിന്നെ കണ്ടതിൽ ഒരുപാട് സന്തോഷം."},
            {"ml-sweet-girl-phone-happy","ml-IN","sweet_girl","phone:","happy","നിന്നെ കണ്ടതിൽ ഒരുപാട് സന്തോഷം."},
            {"ml-sweet-girl-ai-angry","ml-IN","sweet_girl","","angry","ഇത് ഒട്ടും ശരിയല്ല!"},
            {"ml-sweet-girl-ai-sad","ml-IN","sweet_girl","","sad","എനിക്ക് വിഷമമുണ്ട്. അത് ശരിക്കും ബുദ്ധിമുട്ടാണ്."},
        };
        for(String[] k:cases){
            String lang=k[1].substring(0,2);p.edit().putString("voice_language",k[1]).putString("voice_character",k[2]).putString("voice_name_"+lang,k[3]).putInt("voice_emotion",100).putBoolean("voice_sounds",true).commit();
            java.util.concurrent.CountDownLatch configured=new java.util.concurrent.CountDownLatch(1);sp[0].refresh(configured::countDown);assertTrue(configured.await(60,java.util.concurrent.TimeUnit.SECONDS));
            assertTrue(k[0]+": no voice",sp[0].ready());
            String sound=MiloVoice.interjection(k[4],lang.equals("ml"));long t0=android.os.SystemClock.elapsedRealtime();
            MiloSpeaker.Clip lead=sound==null?null:sp[0].clip(sound,k[4],"t-lead"),main=sp[0].clip(k[5],k[4],"t-main");long ms=android.os.SystemClock.elapsedRealtime()-t0;
            assertNotNull(k[0]+": nothing rendered",main);assertTrue(k[0]+": too short",main.pcm.length>main.rate/3);
            if(k[0].contains("-ai-"))assertTrue(k[0]+" should use an AI voice, used "+sp[0].engine(),sp[0].engine().startsWith("ai:"));
            if(k[0].contains("-phone-"))assertEquals("phone",sp[0].engine());
            short[] all=new short[(lead==null?0:lead.pcm.length+lead.rate/8)+main.pcm.length];if(lead!=null)System.arraycopy(lead.pcm,0,all,0,lead.pcm.length);System.arraycopy(main.pcm,0,all,all.length-main.pcm.length,main.pcm.length);
            writeWav(new java.io.File(out,k[0]+".wav"),all,Math.round(main.rate*main.playback));
            android.util.Log.i("MiloVoiceTest",k[0]+" engine="+sp[0].engine()+" render="+ms+"ms audio="+(all.length*1000L/main.rate)+"ms");
        }
        for(String key:p.getAll().keySet())if(key.startsWith("voice_f0_"))android.util.Log.i("MiloVoiceTest",key+"="+p.getFloat(key,0));
        // Real playback with lip-sync levels and callbacks.
        p.edit().putString("voice_language","en-US").putString("voice_character","sweet_girl").putString("voice_name_en","").commit();
        sp[0].refresh(null);sp[0].beginReply();sp[0].speak("Hello! This is Milo's new voice.","happy","probe-final");
        assertTrue("speech did not finish",played.await(30,java.util.concurrent.TimeUnit.SECONDS));
        assertTrue("onStart should fire for the sound and the sentence",starts[0]>=2);assertTrue("lip sync levels should rise, max="+maxLevel[0],maxLevel[0]>0.2f);
        sp[0].shutdown();
    }
    static void writeWav(java.io.File f,short[] pcm,int rate)throws java.io.IOException{
        try(java.io.DataOutputStream o=new java.io.DataOutputStream(new java.io.BufferedOutputStream(new java.io.FileOutputStream(f)))){
            int bytes=pcm.length*2;o.writeBytes("RIFF");o.writeInt(Integer.reverseBytes(36+bytes));o.writeBytes("WAVEfmt ");o.writeInt(Integer.reverseBytes(16));o.writeShort(Short.reverseBytes((short)1));o.writeShort(Short.reverseBytes((short)1));
            o.writeInt(Integer.reverseBytes(rate));o.writeInt(Integer.reverseBytes(rate*2));o.writeShort(Short.reverseBytes((short)2));o.writeShort(Short.reverseBytes((short)16));o.writeBytes("data");o.writeInt(Integer.reverseBytes(bytes));
            for(short s:pcm)o.writeShort(Short.reverseBytes(s));
        }
    }
    public void testToneTagIsHiddenAndParsed(){
        assertEquals("playful",ToneTag.tone("[playful] Hello there"));assertEquals("Hello there",ToneTag.visible("[playful] Hello there"));
        assertNull("tag may still be arriving",ToneTag.visible("[play"));assertEquals("Hi",ToneTag.visible("Hi"));
        assertNull(ToneTag.tone("[grumpy] no"));assertEquals("no",ToneTag.visible("[grumpy] no"));
        assertEquals("angry",ToneTag.tone("[angry] Hmph"));assertEquals("Haha! Oh no.",ToneTag.visible("[laughing] Haha! [sad] Oh no."));assertEquals("sad",ToneTag.lastTone("[laughing] Haha! [sad] Oh no."));
        assertEquals("Hello ",ToneTag.visible("Hello [sa"));
    }
    public void testGeminiQuotaGuidanceKeepsSavedKey() {
        String message=ProviderErrors.classify("Google",429,"RESOURCE_EXHAUSTED");
        assertTrue(message.contains("project's rate or daily quota"));
        assertTrue(message.contains("key is saved"));
        assertTrue(message.contains("on-phone AI"));
        assertFalse(message.contains("Paste an active"));
    }
    public void testPowerControlSavesAcrossReopen() throws Throwable {
        MainActivity a=getActivity();runTestOnUiThread(()->{
            a.setPetEnabled(false);
            assertEquals("Turn on",a.powerButton.getText().toString());
            assertFalse(a.getSharedPreferences("milo",0).getBoolean("pet_enabled",true));
            a.setPetEnabled(true);
            assertEquals("Turn off",a.powerButton.getText().toString());
            assertTrue(a.getSharedPreferences("milo",0).getBoolean("pet_enabled",false));
            a.show("Talk");assertFalse(a.petFace.expression().equals("off"));
        });
        assertTrue(a.getSharedPreferences("milo",0).getBoolean("pet_enabled",false));
    }
    public void testPhoneSpeakerAndSpeechInputAvailable() throws Throwable {
        MainActivity a=getActivity();
        long deadline=android.os.SystemClock.uptimeMillis()+10000;
        while(!a.voiceReady&&android.os.SystemClock.uptimeMillis()<deadline)android.os.SystemClock.sleep(100);
        assertTrue("Android text-to-speech voice did not initialize",a.voiceReady);
        android.content.Intent speech=new android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        assertNotNull("No Android speech input activity is available",speech.resolveActivity(a.getPackageManager()));
        runTestOnUiThread(()->{
            a.setPetEnabled(true);
            assertTrue("Milo has no voice for this language",a.speech.ready());
            a.speech.speak("Hello, this is a Milo speaker test on your phone.","happy","milo-test-device");
        });
    }
    public void testPeopleLocalListAndCloudActivationState() throws Throwable {
        MainActivity a=getActivity();
        runTestOnUiThread(()->{
            a.prefs.edit().putString("people_drafts","[{\"alias\":\"Mom\",\"email\":\"mom@example.com\"}]").commit();
            a.show("People");assertNotNull(a.peopleScreen);assertTrue(a.peopleScreen.live());
            assertEquals(1,a.peopleScreen.drafts().length());
            assertEquals(PeopleClient.configured(a),PeopleClient.ready(a));assertTrue(a.peopleScreen instanceof SparkPeopleScreen);
            PeopleScreen old=a.peopleScreen;a.show("Talk");assertTrue(old.closed);assertNull(a.peopleScreen);
            a.prefs.edit().remove("people_drafts").commit();
        });
        android.content.pm.PackageInfo info=a.getPackageManager().getPackageInfo(a.getPackageName(),android.content.pm.PackageManager.GET_PERMISSIONS);
        if(info.requestedPermissions!=null)for(String permission:info.requestedPermissions){assertFalse("People must not request location",permission.equals("android.permission.ACCESS_FINE_LOCATION")||permission.equals("android.permission.ACCESS_COARSE_LOCATION")||permission.equals("android.permission.ACCESS_BACKGROUND_LOCATION"));}
    }
    public void testFloatingPetStartsUpdatesAndStopsWithOff() throws Throwable {
        MainActivity a=getActivity();
        assertTrue("Test emulator must grant overlay permission",android.provider.Settings.canDrawOverlays(a));
        runTestOnUiThread(()->{a.prefs.edit().putBoolean("pet_enabled",true).commit();a.launchFloatingPet();});
        long end=android.os.SystemClock.uptimeMillis()+5000;
        while(!FloatingPetService.visible&&android.os.SystemClock.uptimeMillis()<end)android.os.SystemClock.sleep(100);
        assertTrue("Floating window was not added",FloatingPetService.visible);
        runTestOnUiThread(()->a.moveTaskToBack(true));
        android.os.SystemClock.sleep(1100);
        assertTrue("Floating pet must remain while app is in background",FloatingPetService.visible);
        android.graphics.Bitmap shot=getInstrumentation().getUiAutomation().takeScreenshot();
        assertNotNull(shot);
        try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(a.getExternalFilesDir(null),"milo-floating.png"))){shot.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}finally{shot.recycle();}
        runTestOnUiThread(()->a.setExpression("celebrate"));
        assertEquals("celebrate",a.prefs.getString("pet_expression",""));
        runTestOnUiThread(()->a.prefs.edit().putBoolean("pet_enabled",false).commit());
        end=android.os.SystemClock.uptimeMillis()+5000;
        while(FloatingPetService.visible&&android.os.SystemClock.uptimeMillis()<end)android.os.SystemClock.sleep(100);
        assertFalse("Off must remove the window",FloatingPetService.visible);
        runTestOnUiThread(()->a.prefs.edit().putBoolean("pet_enabled",true).commit());
    }
    public void testLocalSdkAvailabilityReturnsAnActionableStatus() throws Throwable {
        getActivity();java.util.concurrent.CountDownLatch done=new java.util.concurrent.CountDownLatch(1);AtomicReference<LocalModelSupport.Status> value=new AtomicReference<>();
        LocalModelSupport.check(result->{value.set(result);done.countDown();});
        assertTrue("Local readiness check did not return",done.await(40,java.util.concurrent.TimeUnit.SECONDS));assertNotNull(value.get());assertTrue(value.get().message.length()>20);
    }
    public void testProviderSetupSavesEncryptedKeysAndDisconnects() throws Throwable {
        MainActivity a=getActivity();runTestOnUiThread(()->{
            for(String pid:new String[]{AiProvider.GEMINI,AiProvider.OPENAI,AiProvider.CLAUDE}){
                // A real phone may already have a private connection. Never replace or disconnect it.
                if(KeyVault.hasKeyForProvider(a,pid))continue;
                android.widget.LinearLayout area=new android.widget.LinearLayout(a);a.renderProviderSetup(area,pid);
                android.widget.EditText key=null;android.widget.CheckBox consent=null;android.widget.Button save=null,disconnect=null;
                for(int i=0;i<area.getChildCount();i++){android.view.View v=area.getChildAt(i);if(v instanceof android.widget.EditText&&((android.widget.EditText)v).getHint().toString().contains("API key"))key=(android.widget.EditText)v;if(v instanceof android.widget.CheckBox)consent=(android.widget.CheckBox)v;if(v instanceof android.widget.Button){String label=((android.widget.Button)v).getText().toString();if(label.equals("Save connection"))save=(android.widget.Button)v;if(label.equals("Disconnect this provider"))disconnect=(android.widget.Button)v;}}
                assertNotNull(key);assertNotNull(consent);assertNotNull(save);assertNotNull(disconnect);
                key.setText("fixture-not-real-"+pid);save.performClick();assertFalse(KeyVault.hasKeyForProvider(a,pid));consent.setChecked(true);save.performClick();
                try{assertEquals("fixture-not-real-"+pid,KeyVault.loadForProvider(a,pid));}catch(Exception e){throw new AssertionError(e);}
                assertEquals("",key.getText().toString());save.performClick();try{assertEquals("fixture-not-real-"+pid,KeyVault.loadForProvider(a,pid));}catch(Exception e){throw new AssertionError(e);}assertFalse(a.prefs.getString("key_data_"+pid,"").contains("fixture-not-real"));disconnect.performClick();assertFalse(KeyVault.hasKeyForProvider(a,pid));
            }
        });
    }
    public void testCloudStreamsRejectIncompleteReplies() throws Exception {
        String open="data: {\"choices\":[{\"index\":0,\"delta\":{\"content\":\"Hello\"}}]}\n\n";
        String end="data: {\"choices\":[{\"index\":0,\"delta\":{},\"finish_reason\":\"stop\"}]}\n\ndata: [DONE]\n\n";
        assertEquals("Hello",AiProvider.parseOpenAiSse(new java.io.BufferedReader(new java.io.StringReader(open+end)),s->{}).text);
        try{AiProvider.parseOpenAiSse(new java.io.BufferedReader(new java.io.StringReader(open)),s->{});fail("Incomplete OpenAI stream accepted");}catch(java.io.IOException expected){}
        String claude="data: {\"type\":\"content_block_delta\",\"delta\":{\"type\":\"text_delta\",\"text\":\"Hello\"}}\n\n";
        String finish="data: {\"type\":\"message_delta\",\"delta\":{\"stop_reason\":\"end_turn\"}}\n\ndata: {\"type\":\"message_stop\"}\n\n";
        assertEquals("Hello",ClaudeClient.parseClaudeSse(new java.io.BufferedReader(new java.io.StringReader(claude+finish)),s->{}).text);
        try{ClaudeClient.parseClaudeSse(new java.io.BufferedReader(new java.io.StringReader(claude)),s->{});fail("Incomplete Claude stream accepted");}catch(java.io.IOException expected){}
        try{AiProvider.validateKey("bad\nkey",true);fail("Header injection accepted");}catch(MainActivity.ApiFailure expected){}
    }
    public void testGeminiAutoModelSelectionAndOldModelMigration() throws Throwable {
        org.json.JSONArray available=new org.json.JSONArray()
            .put(new org.json.JSONObject().put("name","models/gemini-2.5-flash").put("supportedGenerationMethods",new org.json.JSONArray().put("generateContent")))
            .put(new org.json.JSONObject().put("name","models/gemini-3.5-flash-lite").put("supportedGenerationMethods",new org.json.JSONArray().put("generateContent")))
            .put(new org.json.JSONObject().put("name","models/gemini-3.8-flash").put("supportedGenerationMethods",new org.json.JSONArray().put("generateContent")))
            .put(new org.json.JSONObject().put("name","models/gemini-3.1-flash-lite").put("supportedGenerationMethods",new org.json.JSONArray().put("embedContent")));
        assertEquals("gemini-3.5-flash-lite",GeminiModels.choose(available,false));
        assertEquals("gemini-3.8-flash",GeminiModels.choose(available,true));
        assertEquals("",GeminiModels.choose(new org.json.JSONArray().put(new org.json.JSONObject().put("name","models/gemini-3.5-flash-lite").put("supportedGenerationMethods",new org.json.JSONArray().put("embedContent"))),false));
        MainActivity a=getActivity();runTestOnUiThread(()->{
            a.prefs.edit().putString("ai_provider",AiProvider.GEMINI).putString("model_gemini","gemini-2.5-flash").commit();
            assertEquals("auto",((GeminiProvider)AiProvider.current(a)).model);
            assertEquals("auto",a.prefs.getString("model_gemini",""));
        });
    }
    public void testLocalDefaultAndModelStatusWithoutCloudFallback() throws Throwable {
        MainActivity a=getActivity();runTestOnUiThread(()->{
            a.prefs.edit().remove("ai_provider").commit();assertEquals(AiProvider.LOCAL,AiProvider.current(a).id());assertFalse(AiProvider.current(a).needsNetwork());
            assertEquals(LocalModelSupport.State.UNAVAILABLE,LocalModelSupport.status(com.google.mlkit.genai.common.FeatureStatus.UNAVAILABLE).state);
            assertEquals(LocalModelSupport.State.DOWNLOADABLE,LocalModelSupport.status(com.google.mlkit.genai.common.FeatureStatus.DOWNLOADABLE).state);
            a.show("Talk");assertFalse(a.search.isEnabled());a.prefs.edit().putBoolean("pet_enabled",false).commit();assertFalse(a.canSpeak(a.requestId));a.prefs.edit().putBoolean("pet_enabled",true).commit();
        });
    }
    public void testExpressionsFollowMoodAndRespectOff() throws Throwable {
        MainActivity a=getActivity();
        runTestOnUiThread(()->{
            a.prefs.edit().putBoolean("pet_enabled",true).commit();
            a.mood="Stressed";a.show("Talk");assertEquals("comfort",a.petFace.expression());
            a.setExpression("thinking");assertEquals("Thinking",a.expressionLabel.getText().toString());
            a.setExpression("listening");assertEquals("listening",a.petFace.expression());
            a.prefs.edit().putBoolean("pet_enabled",false).commit();
            a.setExpression("speaking");assertEquals("off",a.petFace.expression());
            a.show("Goals");assertEquals("off",a.petFace.expression());
            assertFalse(a.busy);
            a.prefs.edit().putBoolean("pet_enabled",true).commit();a.mood="Excited";
            a.show("Talk");assertEquals("celebrate",a.petFace.expression());
            a.prefs.edit().putString("companion_style","Playful").commit();a.show("Home");
            assertTrue(a.home.getUrl().contains("style=Playful"));
            a.prefs.edit().remove("companion_style").commit();a.mood="Okay";
        });
    }
    public void testStreamingKeepsSourcesAndRejectsIncompleteReplies() throws Exception {
        String first="data: {\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"Hello. \"}]}}]}\n\n";
        String last="data: {\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"Small steps count.\"}]},\"finishReason\":\"STOP\",\"groundingMetadata\":{\"groundingChunks\":[{\"web\":{\"uri\":\"https://example.org\",\"title\":\"Example\"}}]}}]}\n\n";
        java.util.List<String> chunks=new java.util.ArrayList<>();
        MainActivity.GeminiReply reply=GeminiStream.read(new java.io.BufferedReader(new java.io.StringReader(first+last)),chunks::add);
        assertEquals("Hello. Small steps count.",reply.text);assertEquals(2,chunks.size());assertEquals(1,reply.sources.length());
        try{GeminiStream.read(new java.io.BufferedReader(new java.io.StringReader(first)),s->{});fail("Incomplete stream must be rejected");}catch(java.io.IOException expected){}
        assertTrue(MainActivity.sentenceBoundary("Take a small step. More",0)>0);
    }
    public void testScreensAndEncryptedKey() throws Throwable {
        MainActivity a=getActivity();
        AtomicReference<Throwable> error=new AtomicReference<>();
        runTestOnUiThread(()->{try{
            a.show("Home");assertNotNull(a.home);
            a.show("Talk");assertNotNull(a.input);assertTrue(a.send.isEnabled());
            a.show("Comfort");assertTrue(a.content.getChildCount()>0);
            a.show("Goals");assertTrue(a.content.getChildCount()>0);
            a.show("Memories");assertNotNull(a.memoryCaption);assertTrue(a.content.getChildCount()>0);
            a.saveKey("test-key-not-a-real-credential");
            assertEquals("test-key-not-a-real-credential",a.loadKey());
            assertFalse(a.prefs.getString("key_data","").contains("test-key"));
            a.prefs.edit().remove("key_data").remove("key_iv").commit();
            assertEquals("",a.loadKey());
            a.prefs.edit().putString("pet_name","Leafy").commit();assertEquals("Leafy",a.petName());
            a.show("Home");assertTrue(a.home.getUrl().contains("pet=Leafy"));
            a.prefs.edit().remove("pet_name").commit();
        }catch(Throwable t){error.set(t);}});
        if(error.get()!=null)throw error.get();
    }
    public void testOffBlocksMessagesAndDoesNotRequestMicrophone() throws Throwable {
        MainActivity a=getActivity();
        runTestOnUiThread(()->{a.prefs.edit().putBoolean("pet_enabled",false).commit();a.show("Talk");a.input.setText("A test question");a.sendMessage();assertFalse(a.busy);assertEquals("Your pet is off. Enable it in Setup.",a.status.getText().toString());a.prefs.edit().remove("pet_enabled").commit();});
    }
    public void testTalkScreenCapture() throws Throwable {
        MainActivity a=getActivity();runTestOnUiThread(()->a.show("Talk"));getInstrumentation().waitForIdleSync();
        android.widget.LinearLayout header=(android.widget.LinearLayout)a.root.getChildAt(0);
        assertTrue(header.getChildAt(0).getWidth()>0);
        assertTrue(header.getChildAt(2).getRight()<=header.getWidth());
        android.graphics.Bitmap shot=getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(shot);
        java.io.File file=new java.io.File(getInstrumentation().getTargetContext().getExternalFilesDir(null),"milo-talk.png");
        try(java.io.FileOutputStream out=new java.io.FileOutputStream(file)){assertTrue(shot.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out));}shot.recycle();
    }
}
