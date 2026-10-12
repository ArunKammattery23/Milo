package life.milo.companion;

import com.google.android.gms.tasks.*;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;
import java.util.*;

/** Spark transport: all identity, consent, quotas and immutable fields are checked by rules. */
final class SparkPeople {
    static FirebaseFirestore db(){return PeopleClient.db();}
    static FirebaseUser user(){return PeopleClient.current();}
    static DocumentReference profile(){return db().collection("sparkUsers").document(user().getUid());}
    static DocumentReference room(String id){return db().collection("sparkRooms").document(id);}
    static Map<String,Object> data(Object... p){return PeopleClient.data(p);}
    static Task<Void> saveName(String name){return profile().set(data("name",name,"email",user().getEmail(),"active",true));}
    static void quota(Transaction tx,String uid,String id) throws FirebaseFirestoreException {
        DocumentReference ref=db().document("sparkUsers/"+uid+"/limits/actions");DocumentSnapshot old=tx.get(ref);
        Timestamp start=old.getTimestamp("start");boolean reset=start==null||System.currentTimeMillis()-start.toDate().getTime()>=86400000;
        long count=reset?1:old.getLong("count")+1;
        if(count>120)throw new FirebaseFirestoreException("Daily People allowance reached. Try again tomorrow.",FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED);
        tx.set(ref,data("start",reset?FieldValue.serverTimestamp():start,"count",count,"lastId",id,"lastAt",FieldValue.serverTimestamp()));
    }
    static Task<String> invite(String email,String alias){
        FirebaseUser u=user();String code=UUID.randomUUID().toString().replace("-","");
        return db().runTransaction(tx->{DocumentSnapshot p=tx.get(db().document("sparkUsers/"+u.getUid()));
            quota(tx,u.getUid(),"invite/"+code);
            tx.set(db().document("sparkInvites/"+code),data("fromUid",u.getUid(),"fromName",p.getString("name"),"fromEmail",u.getEmail(),"toEmail",email,"alias",alias,"createdAt",FieldValue.serverTimestamp(),"expiresAt",new Timestamp(new Date(System.currentTimeMillis()+23*3600000)),"acceptedBy",""));return code;});
    }
    static Task<Void> accept(String code,String alias){
        FirebaseUser u=user();DocumentReference invite=db().document("sparkInvites/"+code);
        return db().runTransaction(tx->{DocumentSnapshot i=tx.get(invite),p=tx.get(db().document("sparkUsers/"+u.getUid()));
            if(!i.exists()||!"".equals(i.getString("acceptedBy")))throw new FirebaseFirestoreException("Invitation unavailable or already used.",FirebaseFirestoreException.Code.FAILED_PRECONDITION);
            tx.update(invite,"acceptedBy",u.getUid());
            tx.set(room(code),data("members",Arrays.asList(i.getString("fromUid"),u.getUid()),"fromUid",i.getString("fromUid"),"toUid",u.getUid(),"fromName",i.getString("fromName"),"fromEmail",i.getString("fromEmail"),"toName",p.getString("name"),"toEmail",u.getEmail(),"fromAlias",i.getString("alias"),"toAlias",alias,"active",true,"createdAt",FieldValue.serverTimestamp()));return null;});
    }
    static Task<Void> send(String channel,String id,String text){
        String uid=user().getUid();DocumentReference ref=room(channel).collection("messages").document(id);
        return db().runTransaction(tx->{DocumentSnapshot existing=tx.get(ref);
            if(existing.exists()){
                if(!uid.equals(existing.getString("fromUid"))||!text.equals(existing.getString("text")))throw new FirebaseFirestoreException("Retry does not match the stored message.",FirebaseFirestoreException.Code.ALREADY_EXISTS);
                return null;
            }
            quota(tx,uid,channel+"/"+id);tx.set(ref,data("fromUid",uid,"text",text,"createdAt",FieldValue.serverTimestamp(),"readAt",null,"hiddenBy",Collections.emptyList()));return null;});
    }
    static Task<Void> remove(DocumentSnapshot channel,boolean block){
        WriteBatch b=db().batch();b.update(channel.getReference(),"active",false);
        if(block)b.set(profile().collection("blocks").document(peer(channel)),data("createdAt",FieldValue.serverTimestamp(),"blocked",true));return b.commit();
    }
    static String peer(DocumentSnapshot r){return user().getUid().equals(r.getString("fromUid"))?r.getString("toUid"):r.getString("fromUid");}
    static String label(DocumentSnapshot r){return r.getString(user().getUid().equals(r.getString("fromUid"))?"fromAlias":"toAlias");}
    static String email(DocumentSnapshot r){return r.getString(user().getUid().equals(r.getString("fromUid"))?"toEmail":"fromEmail");}
    static String error(Exception e){
        if(e instanceof FirebaseFirestoreException){switch(((FirebaseFirestoreException)e).getCode()){
            case PERMISSION_DENIED:return "Not allowed. Verify your email and save your People name. An invitation may be expired, for another email, or blocked.";
            case RESOURCE_EXHAUSTED:return "The free service allowance is reached. Please try later.";
            case UNAVAILABLE:return "You are offline or the service is unavailable. Keep Milo open and try again.";
            case FAILED_PRECONDITION:return "This action is unavailable. Check the invitation and connection.";
            default:break;
        }}return "Could not finish. Check your connection and try again. Your AI key is not used for People.";
    }
}
