"use strict";
const {test,before,after,beforeEach}=require("node:test");
const assert=require("node:assert/strict");
const fs=require("node:fs");
const {randomUUID}=require("node:crypto");
const {initializeApp,deleteApp}=require("firebase-admin/app");
const {getFirestore,Timestamp}=require("firebase-admin/firestore");
const {initializeTestEnvironment,assertFails,assertSucceeds}=require("@firebase/rules-unit-testing");
const {doc,getDoc,setDoc,collection,getDocs}=require("firebase/firestore");
const {createRelay,hash,identity}=require("../relay");
let env,app,db,relay;
const request=(uid,data={},extra={})=>({auth:{uid,token:{email:`${uid}@example.com`,email_verified:true,auth_time:Math.floor(Date.now()/1000)}},app:{appId:"test"},data,...extra});
before(async()=>{
 if(!process.env.FIRESTORE_EMULATOR_HOST)throw new Error("Run through npm test in cloud; production is never used.");
 const [host,port]=process.env.FIRESTORE_EMULATOR_HOST.split(":");
 env=await initializeTestEnvironment({projectId:"demo-milo-people",firestore:{host,port:Number(port),rules:fs.readFileSync(require("node:path").join(__dirname,"../../firestore.rules"),"utf8")}});
 app=initializeApp({projectId:"demo-milo-people"},"relay-tests");db=getFirestore(app);relay=createRelay(db,{deleteUser:async()=>{}});
});
beforeEach(async()=>env.clearFirestore());
after(async()=>{if(env)await env.cleanup();if(app)await deleteApp(app);});
async function profiles(){for(const uid of ["mom","son","outsider"])await relay.saveProfile(request(uid,{name:uid}));}
async function connect(){await profiles();const inv=await relay.createInvite(request("mom",{email:"son@example.com",alias:"Son"}));await relay.acceptInvite(request("son",{code:inv.code,alias:"Mom"}));return inv;}
const reject=(call,code)=>assert.rejects(call,e=>e.code===code);

test("identity requires sign-in, verified email and App Check",()=>{
 assert.throws(()=>identity(request("son",{}, {auth:null})),e=>e.code==="unauthenticated");
 assert.throws(()=>identity(request("son",{}, {app:null})),e=>e.code==="failed-precondition");
 assert.throws(()=>identity(request("son",{}, {auth:{uid:"son",token:{email:"son@example.com",email_verified:false}}})),e=>e.code==="permission-denied");
});
test("invitations are email-bound, expire, and are single-use",async()=>{
 await profiles();let invite=await relay.createInvite(request("mom",{email:"son@example.com",alias:"Son"}));
 await reject(relay.acceptInvite(request("outsider",{code:invite.code,alias:"Mom"})),"permission-denied");
 await relay.acceptInvite(request("son",{code:invite.code,alias:"Mom"}));
 await reject(relay.acceptInvite(request("son",{code:invite.code,alias:"Mom"})),"permission-denied");
 invite=await relay.createInvite(request("mom",{email:"outsider@example.com",alias:"Friend"}));
 await db.doc(`invites/${hash(invite.code)}`).update({expiresAt:Timestamp.fromMillis(Date.now()-1)});
 await reject(relay.acceptInvite(request("outsider",{code:invite.code,alias:"Mom"})),"permission-denied");
});
test("exact messages use the authenticated sender, duplicate sends are idempotent, recipient-only read receipts",async()=>{
 await connect();const data={peerUid:"son",text:"Where are you? Please reply when you can.",requestId:randomUUID(),fromUid:"outsider",fromName:"Fake"};
 const results=await Promise.all([relay.sendMessage(request("mom",data)),relay.sendMessage(request("mom",data))]);assert.equal(results[0].messageId,results[1].messageId);
 const id=results[0].messageId,m=await db.doc(`users/son/messages/${id}`).get();assert.equal(m.get("fromUid"),"mom");assert.equal(m.get("fromName"),"mom");assert.equal(m.get("text"),data.text);
 assert.equal((await db.collection("users/son/messages").get()).size,1);
 await reject(relay.sendMessage(request("mom",{...data,text:"Changed words"})),"already-exists");
 await reject(relay.markRead(request("mom",{messageId:id})),"permission-denied");
 await relay.markRead(request("son",{messageId:id}));assert.ok((await db.doc(`users/mom/messages/${id}`).get()).get("readAt"));
});
test("unconnected and blocked people cannot send; unblock still requires a new invitation",async()=>{
 await connect();const data={peerUid:"son",text:"Please come home soon.",requestId:randomUUID()};
 await reject(relay.sendMessage(request("outsider",data)),"permission-denied");
 await relay.manageConnection(request("son",{peerUid:"mom",action:"block"}));
 await reject(relay.sendMessage(request("mom",data)),"permission-denied");
 const inv=await relay.createInvite(request("mom",{email:"son@example.com",alias:"Son"}));
 await reject(relay.acceptInvite(request("son",{code:inv.code,alias:"Mom"})),"permission-denied");
 await relay.manageConnection(request("son",{peerUid:"mom",action:"unblock"}));
 await reject(relay.sendMessage(request("mom",data)),"permission-denied");
 await relay.acceptInvite(request("son",{code:inv.code,alias:"Mom"}));await relay.sendMessage(request("mom",data));
});
test("daily quota and maximum message length are enforced",async()=>{
 await connect();const date=new Date().toISOString().slice(0,10);await db.doc(`users/mom/limits/${date}`).set({send:100});
 await reject(relay.sendMessage(request("mom",{peerUid:"son",text:"Hello",requestId:randomUUID()})),"resource-exhausted");
 await reject(relay.sendMessage(request("son",{peerUid:"mom",text:"x".repeat(2001),requestId:randomUUID()})),"invalid-argument");
});
test("push token ownership moves between accounts and old account cannot unregister it",async()=>{
 await profiles();const token="test-device-token";
 await relay.registerDevice(request("mom",{token,ownerUid:"mom"}));await relay.registerDevice(request("son",{token,ownerUid:"son"}));
 await reject(relay.registerDevice(request("son",{token,ownerUid:"mom"})),"permission-denied");
 assert.equal((await db.doc(`users/mom/devices/${hash(token)}`).get()).exists,false);
 await relay.unregisterDevice(request("mom",{token}));assert.equal((await db.doc(`pushOwners/${hash(token)}`).get()).get("uid"),"son");
});
test("deleting a copy preserves the other copy; deleted accounts cannot recreate profiles with stale tokens",async()=>{
 await connect();const sent=await relay.sendMessage(request("mom",{peerUid:"son",text:"Hello",requestId:randomUUID()}));
 await relay.deleteMessage(request("son",{messageId:sent.messageId}));assert.ok((await db.doc(`users/mom/messages/${sent.messageId}`).get()).exists);
 await relay.deleteAccount(request("son"));await reject(relay.saveProfile(request("son",{name:"Recreated"})),"permission-denied");
 await reject(relay.sendMessage(request("mom",{peerUid:"son",text:"Hello again",requestId:randomUUID()})),"permission-denied");
});
test("Firestore denies unauthenticated, unverified, cross-account reads and all client writes",async()=>{
 await connect();const own=env.authenticatedContext("mom",{email_verified:true}).firestore();const stranger=env.authenticatedContext("outsider",{email_verified:true}).firestore();const unverified=env.authenticatedContext("mom",{email_verified:false}).firestore();
 await assertSucceeds(getDoc(doc(own,"users/mom")));await assertSucceeds(getDocs(collection(own,"users/mom/contacts")));
 await assertFails(getDoc(doc(stranger,"users/mom")));await assertFails(getDocs(collection(stranger,"users/mom/messages")));
 await assertFails(getDoc(doc(unverified,"users/mom")));await assertFails(getDoc(doc(env.unauthenticatedContext().firestore(),"users/mom")));
 for(const path of ["users/mom","users/mom/messages/fake","users/mom/contacts/fake","connections/fake","invites/fake","pushOwners/fake","deletedUsers/mom"])await assertFails(setDoc(doc(own,path),{active:true}));
 await assertFails(getDocs(collection(own,"users")));await assertFails(getDocs(collection(own,"invites")));await assertFails(getDocs(collection(own,"users/mom/devices")));
});
