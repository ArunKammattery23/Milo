package life.milo.companion;
import android.app.AlertDialog;
import android.content.*;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.widget.*;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.*;
import java.util.*;

/** Small, explicit conversations. Only the visible People view has a live listener. */
final class SparkPeopleScreen extends PeopleScreen {
    DocumentSnapshot selected;
    boolean listening;
    SparkPeopleScreen(MainActivity activity){super(activity);}
    @Override void show(){listening=true;
        LinearLayout page=a.scrollPage();page.addView(a.text("Your people",26));
        page.addView(a.text("Messages update while this screen is open. No AI key needed. Choose a person to read, listen or reply.",14));
        feedback=a.text("",13);page.addView(feedback);action(page,"Add a person",this::addDraft);showDrafts(page);
        if(!PeopleClient.ready(a)){page.addView(a.text("This build needs the owner's Firebase app configuration. Saved names work offline.",14));return;}
        FirebaseUser u=SparkPeople.user();if(u==null){showLogin(page);return;}
        page.addView(a.text("Signed in as "+u.getEmail(),12));
        if(!u.isEmailVerified()){
            page.addView(a.text("Open your verification email before connecting.",16));
            action(page,"Send verification email",()->u.sendEmailVerification().addOnSuccessListener(v->say("Check your email for the verification link.")).addOnFailureListener(e->say("Could not send verification email. Try later.")));
            action(page,"I have verified · refresh",()->u.reload().addOnSuccessListener(v->u.getIdToken(true).addOnSuccessListener(t->refresh())));
            action(page,"Sign out",this::signOut);return;
        }
        EditText name=a.edit("Your People name",false);name.setText(u.getDisplayName()==null?"":u.getDisplayName());page.addView(name);
        action(page,"Save my People name",()->{String value=name.getText().toString().trim();if(!validName(value)){name.setError("Use 1–40 characters");return;}SparkPeople.saveName(value).addOnSuccessListener(v->{u.updateProfile(new UserProfileChangeRequest.Builder().setDisplayName(value).build());refresh();}).addOnFailureListener(e->say(SparkPeople.error(e)));});
        action(page,"Invite family or a friend",()->invite("",""));action(page,"Accept an invitation",this::accept);
        action(page,"Blocked people",this::blocked);
        page.addView(a.text("Connections",20));contacts=new LinearLayout(a);contacts.setOrientation(LinearLayout.VERTICAL);page.addView(contacts);
        contactsListener=SparkPeople.db().collection("sparkRooms").whereArrayContains("members",u.getUid()).limit(50).addSnapshotListener((rows,error)->{
            if(!live())return;if(error!=null){say(SparkPeople.error(error));return;}contacts.removeAllViews();people.clear();
            if(rows==null||rows.isEmpty()){contacts.addView(a.text("Accept an invitation to start a conversation.",14));return;}
            for(DocumentSnapshot r:rows){people.put(r.getId(),r);action(contacts,SparkPeople.label(r)+(Boolean.TRUE.equals(r.getBoolean("active"))?"":" · Closed"),()->openRoom(r));}
        });
        page.addView(a.text("Cloud messages are private to the two accounts, but are not end-to-end encrypted. Free quotas apply. No background alerts or automatic speaking.",12));
        action(page,"Sign out",this::signOut);
        action(page,"Delete my People account",this::deleteAccount);
    }
    static boolean validName(String n){return !n.isEmpty()&&n.length()<=40;}
    @Override void invite(String email,String alias){
        if(!signedIn())return;LinearLayout f=form();EditText e=a.edit("Their verified email address",false),n=a.edit("Relationship / name, e.g. Son",false);e.setText(email);n.setText(alias);f.addView(e);f.addView(n);
        AlertDialog d=new AlertDialog.Builder(a).setTitle("Invite someone you know").setMessage("Save your People name first. The code is for this email only, lasts 23 hours, and must be accepted. Share it yourself.").setView(f).setNegativeButton("Cancel",null).setPositiveButton("Create code",null).create();
        d.setOnShowListener(x->d.getButton(-1).setOnClickListener(v->{String address=e.getText().toString().trim(),label=n.getText().toString().trim();if(!android.util.Patterns.EMAIL_ADDRESS.matcher(address).matches()||!validName(label)){e.setError("Use their exact account email and a name up to 40 characters");return;}d.getButton(-1).setEnabled(false);
            SparkPeople.invite(address,label).addOnSuccessListener(code->{d.dismiss();if(!live())return;TextView t=a.text(code,17);t.setTextIsSelectable(true);t.setPadding(a.dp(20),a.dp(12),a.dp(20),a.dp(12));new AlertDialog.Builder(a).setTitle("Share this code with "+label).setView(t).setPositiveButton("Copy code",(q,w)->((ClipboardManager)a.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("Milo invitation",code))).setNegativeButton("Close",null).show();}).addOnFailureListener(error->{say(SparkPeople.error(error));d.getButton(-1).setEnabled(true);});}));d.show();
    }
    @Override void accept(){
        if(!signedIn())return;LinearLayout f=form();EditText code=a.edit("Invitation code",false),alias=a.edit("Your name for them, e.g. Mom",false);f.addView(code);f.addView(alias);
        AlertDialog d=new AlertDialog.Builder(a).setTitle("Accept and connect").setMessage("Accept only from someone you know. You will share your name and email and can exchange messages. You can block or close the connection anytime.").setView(f).setNegativeButton("Cancel",null).setPositiveButton("Accept",null).create();
        d.setOnShowListener(x->d.getButton(-1).setOnClickListener(v->{String c=code.getText().toString().trim().toLowerCase(Locale.ROOT),label=alias.getText().toString().trim();if(!c.matches("[a-f0-9]{32}")||!validName(label)){code.setError("Use the complete 32-character code and a name");return;}d.getButton(-1).setEnabled(false);SparkPeople.accept(c,label).addOnSuccessListener(r->{d.dismiss();refresh();}).addOnFailureListener(error->{say(SparkPeople.error(error));d.getButton(-1).setEnabled(true);});}));d.show();
    }
    void openRoom(DocumentSnapshot r){
        listening=true;selected=r;if(contactsListener!=null){contactsListener.remove();contactsListener=null;}if(inboxListener!=null)inboxListener.remove();
        a.content.removeAllViews();LinearLayout page=a.scrollPage();action(page,"‹ Your people",this::refresh);page.addView(a.text(SparkPeople.label(r),25));page.addView(a.text(SparkPeople.email(r),12));feedback=a.text("Opening conversation…",13);page.addView(feedback);
        if(Boolean.TRUE.equals(r.getBoolean("active"))){action(page,"Write or speak a message",()->compose(r.getId(),SparkPeople.label(r),""));action(page,"Ask where they are",()->compose(r.getId(),SparkPeople.label(r),"Where are you? Please reply when you can."));action(page,"Ask them to come home",()->compose(r.getId(),SparkPeople.label(r),"Please come home early. Let me know when you expect to arrive."));
            action(page,"Connection options",()->new AlertDialog.Builder(a).setItems(new String[]{"Close connection","Block this person"},(d,n)->new AlertDialog.Builder(a).setTitle(n==1?"Block this person?":"Close connection?").setMessage("New messages stop. Existing messages remain. A new invitation is needed to reconnect; blocks must first be removed.").setNegativeButton("Cancel",null).setPositiveButton("Confirm",(q,w)->SparkPeople.remove(r,n==1).addOnSuccessListener(v->refresh()).addOnFailureListener(e->say(SparkPeople.error(e)))).show()).show());
        }else page.addView(a.text("This connection is closed. You can still read its messages.",14));
        inbox=new LinearLayout(a);inbox.setOrientation(LinearLayout.VERTICAL);page.addView(inbox);
        inboxListener=r.getReference().collection("messages").orderBy("createdAt",Query.Direction.DESCENDING).limit(50).addSnapshotListener(MetadataChanges.INCLUDE,(rows,error)->{
            if(!live()||!a.foreground)return;if(error!=null){say(SparkPeople.error(error));return;}inbox.removeAllViews();say(rows!=null&&rows.getMetadata().isFromCache()?"Offline view. Connect to the internet to send and receive.":"Connected · messages update here while open");
            if(rows==null||rows.isEmpty()){inbox.addView(a.text("No messages yet.",14));return;}String uid=SparkPeople.user().getUid();
            for(DocumentSnapshot m:rows){List<?> hidden=(List<?>)m.get("hiddenBy");if(hidden!=null&&hidden.contains(uid))continue;boolean mine=uid.equals(m.getString("fromUid"));String text=m.getString("text");action(inbox,(mine?"You":SparkPeople.label(r))+": "+text+"\n"+(mine?(m.getTimestamp("readAt")==null?"Stored · not opened":"Opened"):"Tap to read or listen"),()->message(r,m));}
        });
    }
    @Override void compose(String room,String who,String initial){
        if(!signedIn())return;LinearLayout f=form();EditText body=a.edit("Your message",true);body.setText(initial);body.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(2000)});f.addView(body);
        action(f,"🎙 Speak · review first",()->{dictationTarget=body;Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM).putExtra(RecognizerIntent.EXTRA_LANGUAGE,a.voiceLocale().toLanguageTag()).putExtra(RecognizerIntent.EXTRA_PROMPT,"Message to "+who);if(a.speech!=null)a.speech.stop();a.stopService(new Intent(a,BackgroundVoiceService.class));try{a.startActivityForResult(i,27);}catch(ActivityNotFoundException e){say("Speech unavailable. Type your message.");}});
        String[] pending={"",""};AlertDialog d=new AlertDialog.Builder(a).setTitle("Message to "+who).setView(f).setNegativeButton("Cancel",null).setPositiveButton("Review",null).create();d.setOnDismissListener(x->dictationTarget=null);
        d.setOnShowListener(x->d.getButton(-1).setOnClickListener(v->{String text=body.getText().toString().trim();if(text.isEmpty()){body.setError("Write or speak a message first");return;}new AlertDialog.Builder(a).setTitle("Send to "+who+"?").setMessage(text+"\n\nMilo sends these exact words. They receive it when they open this conversation.").setNegativeButton("Edit",null).setPositiveButton("Send",(q,w)->{if(!pending[1].equals(text)){pending[0]=UUID.randomUUID().toString();pending[1]=text;}d.getButton(-1).setEnabled(false);SparkPeople.send(room,pending[0],text).addOnSuccessListener(t->{d.dismiss();say("Message stored. They can open Milo to read and reply.");}).addOnFailureListener(e->{say(SparkPeople.error(e));d.getButton(-1).setEnabled(true);});}).show();}));d.show();
    }
    void message(DocumentSnapshot r,DocumentSnapshot m){
        String uid=SparkPeople.user().getUid();boolean received=!uid.equals(m.getString("fromUid"));LinearLayout f=form();TextView text=a.text(m.getString("text"),18);text.setTextIsSelectable(true);f.addView(text);
        if(received){action(f,"Read aloud",()->{if(!a.prefs.getBoolean("pet_enabled",true)||!a.voiceReady){say("Enable Milo and check Setup → Test speaker.");return;}a.speech.stop();a.speech.speak(SparkPeople.label(r)+" says: "+m.getString("text"),null,"milo-test-people");});action(f,"Reply",()->{if(a.speech!=null)a.speech.stop();compose(r.getId(),SparkPeople.label(r),"");});}
        action(f,"Hide for me",()->m.getReference().update("hiddenBy",FieldValue.arrayUnion(uid)).addOnSuccessListener(v->say("Hidden from your view. Cloud data remains until the sender deletes it.")).addOnFailureListener(e->say(SparkPeople.error(e))));
        if(!received)action(f,"Delete for both people",()->new AlertDialog.Builder(a).setTitle("Delete your sent message?").setMessage("This removes the stored message for both accounts. Someone may already have read or copied it.").setNegativeButton("Keep",null).setPositiveButton("Delete",(q,w)->m.getReference().delete().addOnFailureListener(e->say(SparkPeople.error(e)))).show());
        AlertDialog d=new AlertDialog.Builder(a).setTitle(received?"Message from "+SparkPeople.label(r):"Your message").setView(f).setPositiveButton("Close",null).create();d.setOnDismissListener(v->{if(a.speech!=null)a.speech.stop();});d.show();
        if(received&&m.getTimestamp("readAt")==null)m.getReference().update("readAt",FieldValue.serverTimestamp()).addOnFailureListener(e->say("Read receipt did not sync. Try again when connected."));
    }
    void blocked(){SparkPeople.profile().collection("blocks").whereEqualTo("blocked",true).get().addOnSuccessListener(rows->{if(!live())return;LinearLayout f=form();if(rows.isEmpty())f.addView(a.text("No blocked people.",14));for(DocumentSnapshot b:rows)action(f,"Unblock account "+b.getId().substring(0,Math.min(6,b.getId().length())),()->new AlertDialog.Builder(a).setTitle("Unblock this account?").setMessage("A new invitation is still required to reconnect.").setNegativeButton("Cancel",null).setPositiveButton("Unblock",(d,w)->b.getReference().update("blocked",false).addOnSuccessListener(v->say("Unblocked. Create a new invitation to reconnect.")).addOnFailureListener(e->say(SparkPeople.error(e)))).show());new AlertDialog.Builder(a).setTitle("Blocked accounts").setView(f).setPositiveButton("Close",null).show();}).addOnFailureListener(e->say(SparkPeople.error(e)));}
    void deleteAccount(){new AlertDialog.Builder(a).setTitle("Delete your People account?").setMessage("Sign in again first if asked. This disables new messages and removes your login and profile details. Existing conversations, names and messages remain for the other person. Delete your sent messages first if you want them removed. Local saved names are erased separately in Setup.").setNegativeButton("Keep",null).setPositiveButton("Delete account",(d,w)->{
        FirebaseUser u=SparkPeople.user();u.getIdToken(true).addOnSuccessListener(token->{Object at=token.getClaims().get("auth_time");if(!(at instanceof Number)||System.currentTimeMillis()/1000-((Number)at).longValue()>300){say("Sign out and sign in again, then retry account deletion.");return;}
            SparkPeople.profile().set(SparkPeople.data("name","Deleted account","email","","active",false)).addOnSuccessListener(v->u.delete().addOnSuccessListener(x->{PeopleClient.signOut(a);refresh();}).addOnFailureListener(e->say("Profile disabled, but login deletion did not finish. Sign in again and retry."))).addOnFailureListener(e->say(SparkPeople.error(e)));
        }).addOnFailureListener(e->say("Sign in again before deleting your account."));}).show();}
    void pause(){if(inboxListener!=null){inboxListener.remove();inboxListener=null;}if(contactsListener!=null){contactsListener.remove();contactsListener=null;}listening=false;}
    void resume(){if(live()&&!listening){listening=true;if(selected!=null)openRoom(selected);else refresh();}}
}
