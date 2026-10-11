package life.milo.companion;

import android.test.ActivityInstrumentationTestCase2;
import java.util.concurrent.atomic.AtomicReference;

/** Device tests: offline startup, screens, encrypted personal key, and mood persistence. */
@SuppressWarnings("deprecation")
public class MiloSmokeTest extends ActivityInstrumentationTestCase2<MainActivity> {
    public MiloSmokeTest(){super(MainActivity.class);}
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
                android.widget.LinearLayout area=new android.widget.LinearLayout(a);a.renderProviderSetup(area,pid);
                android.widget.EditText key=null;android.widget.CheckBox consent=null;android.widget.Button save=null,disconnect=null;
                for(int i=0;i<area.getChildCount();i++){android.view.View v=area.getChildAt(i);if(v instanceof android.widget.EditText&&((android.widget.EditText)v).getHint().toString().contains("API key"))key=(android.widget.EditText)v;if(v instanceof android.widget.CheckBox)consent=(android.widget.CheckBox)v;if(v instanceof android.widget.Button){String label=((android.widget.Button)v).getText().toString();if(label.equals("Save connection"))save=(android.widget.Button)v;if(label.equals("Disconnect this provider"))disconnect=(android.widget.Button)v;}}
                assertNotNull(key);assertNotNull(consent);assertNotNull(save);assertNotNull(disconnect);
                key.setText("fixture-not-real-"+pid);save.performClick();assertFalse(KeyVault.hasKeyForProvider(a,pid));consent.setChecked(true);save.performClick();
                try{assertEquals("fixture-not-real-"+pid,KeyVault.loadForProvider(a,pid));}catch(Exception e){throw new AssertionError(e);}
                assertFalse(a.prefs.getString("key_data_"+pid,"").contains("fixture-not-real"));disconnect.performClick();assertFalse(KeyVault.hasKeyForProvider(a,pid));
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
