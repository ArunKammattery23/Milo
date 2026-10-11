"use strict";
const {initializeApp}=require("firebase-admin/app");
const {getFirestore}=require("firebase-admin/firestore");
const {getAuth}=require("firebase-admin/auth");
const {getMessaging}=require("firebase-admin/messaging");
const {onCall}=require("firebase-functions/v2/https");
const {onDocumentCreated}=require("firebase-functions/v2/firestore");
const {createRelay,active,linkId}=require("./relay");
initializeApp();const db=getFirestore();const relay=createRelay(db,getAuth());
const options={region:"us-central1",enforceAppCheck:true,maxInstances:5,timeoutSeconds:60};
for(const [name,handler] of Object.entries(relay))exports[name]=onCall(options,handler);
exports.notifyRecipient=onDocumentCreated({document:"users/{uid}/messages/{messageId}",region:"us-central1",maxInstances:5,retry:true},async event=>{
 const m=event.data?.data(),uid=event.params.uid;if(!m||m.toUid!==uid||m.expiresAt.toMillis()<=Date.now())return;
 const link=await db.doc(`connections/${linkId(m.fromUid,uid)}`).get();if(!active(link.data(),uid,m.fromUid))return;
 const devices=await db.collection(`users/${uid}/devices`).limit(5).get();if(devices.empty)return;
 const result=await getMessaging().sendEachForMulticast({tokens:devices.docs.map(d=>d.get("token")),data:{kind:"milo_people",ownerUid:uid,messageId:event.params.messageId},android:{priority:"high",ttl:86400000,collapseKey:"milo_people"}});
 let transient=false;for(let i=0;i<result.responses.length;i++){const code=result.responses[i].error?.code;if(["messaging/registration-token-not-registered","messaging/invalid-registration-token"].includes(code))await devices.docs[i].ref.delete();else if(code)transient=true;}if(transient)throw new Error("Push service temporarily unavailable");
});
