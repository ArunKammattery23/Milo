package life.milo.companion;

import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.SystemClock;
import android.util.Log;
import com.k2fsa.sherpa.onnx.*;
import java.io.*;
import java.util.*;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

/**
 * On-phone AI voices through sherpa-onnx: Kokoro (natural English voices such as af_heart) and Piper Meera (Malayalam).
 * Voices run offline on the phone CPU; they are optional downloads stored in Milo's app folder.
 */
final class NeuralVoices {
    static final String TAG="MiloVoiceAI",KOKORO="kokoro",MEERA="meera";
    static final String RELEASE="https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/";
    /** Kokoro v1.0 speaker IDs, in the order stored in the model. */
    static final String[] KOKORO_NAMES={"af_alloy","af_aoede","af_bella","af_heart","af_jessica","af_kore","af_nicole","af_nova","af_river","af_sarah","af_sky",
        "am_adam","am_echo","am_eric","am_fenrir","am_liam","am_michael","am_onyx","am_puck","am_santa","bf_alice","bf_emma","bf_isabella","bf_lily",
        "bm_daniel","bm_fable","bm_george","bm_lewis"};
    /** Friendly names for the English voices offered in Voice Studio. */
    static final String[][] KOKORO_CHOICES={{"af_heart","Heart · warm girl · best quality, slower"},{"af_bella","Bella · bright girl · slower"},{"af_nicole","Nicole · soft whisper · slower"},{"af_sky","Sky · light girl · slower"},
        {"af_sarah","Sarah · gentle · slower"},{"af_nova","Nova · clear · slower"},{"bf_emma","Emma · British girl · slower"},{"am_puck","Puck · cheerful boy · slower"},{"am_michael","Michael · calm boy · slower"}};

    /** Voices live in private internal storage, unpacked by Milo itself, so the speech engine can always read them. */
    static File root(Context c){return new File(c.getNoBackupFilesDir(),"voices");}
    static File dir(Context c,String kind){return new File(root(c),kind);}
    static File espeak(Context c){return new File(root(c),"espeak-ng-data");}
    static boolean installed(Context c,String kind){
        File d=dir(c,kind);if(!new File(espeak(c),"phontab").isFile())return false;
        if(KOKORO.equals(kind))return new File(d,"model.int8.onnx").length()>100_000_000&&new File(d,"voices.bin").isFile()&&new File(d,"tokens.txt").isFile()&&new File(d,"lexicon-us-en.txt").isFile();
        return new File(d,"ml_IN-meera-medium.onnx").length()>50_000_000&&new File(d,"tokens.txt").isFile();
    }
    static int kokoroId(String name){for(int i=0;i<KOKORO_NAMES.length;i++)if(KOKORO_NAMES[i].equals(name))return i;return 3;}

    /* ---- engines: loaded once and kept, since loading takes a moment ---- */
    private static final Map<String,OfflineTts> engines=new HashMap<>();
    static synchronized OfflineTts engine(Context c,String kind){
        OfflineTts tts=engines.get(kind);if(tts!=null)return tts;
        if(!installed(c,kind))return null;
        long start=SystemClock.elapsedRealtime();File d=dir(c,kind);
        OfflineTtsModelConfig model=new OfflineTtsModelConfig();model.setNumThreads(4);model.setProvider("cpu");
        if(KOKORO.equals(kind)){
            OfflineTtsKokoroModelConfig k=new OfflineTtsKokoroModelConfig();
            k.setModel(new File(d,"model.int8.onnx").getPath());k.setVoices(new File(d,"voices.bin").getPath());k.setTokens(new File(d,"tokens.txt").getPath());
            k.setDataDir(espeak(c).getPath());k.setLexicon(new File(d,"lexicon-us-en.txt").getPath());k.setLang("en-us");model.setKokoro(k);
        }else{
            OfflineTtsVitsModelConfig v=new OfflineTtsVitsModelConfig();
            v.setModel(new File(d,"ml_IN-meera-medium.onnx").getPath());v.setTokens(new File(d,"tokens.txt").getPath());v.setDataDir(espeak(c).getPath());model.setVits(v);
        }
        OfflineTtsConfig config=new OfflineTtsConfig();config.setModel(model);config.setMaxNumSentences(1);
        try{tts=new OfflineTts(null,config);}catch(Throwable e){Log.w(TAG,"could not load "+kind+": "+e);return null;}
        engines.put(kind,tts);Log.i(TAG,kind+" voice loaded in "+(SystemClock.elapsedRealtime()-start)+"ms");return tts;
    }
    static synchronized void release(){for(OfflineTts t:engines.values())try{t.release();}catch(Throwable ignored){}engines.clear();}

    /* ---- optional downloads, Wi-Fi only, unpacked straight into Milo's app folder ---- */
    static String archive(String kind){return KOKORO.equals(kind)?"kokoro-int8-multi-lang-v1_0.tar.bz2":"vits-piper-ml_IN-meera-medium.tar.bz2";}
    static String size(String kind){return KOKORO.equals(kind)?"132 MB":"67 MB";}
    static long[] downloadStatus(Context c,String kind){
        long id=c.getSharedPreferences("milo",Context.MODE_PRIVATE).getLong("voice_download_"+kind,-1);if(id<0)return null;
        try(Cursor q=c.getSystemService(DownloadManager.class).query(new DownloadManager.Query().setFilterById(id))){
            if(q==null||!q.moveToFirst())return null;
            return new long[]{q.getInt(q.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)),q.getLong(q.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)),q.getLong(q.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))};
        }
    }
    static void download(Context c,String kind){
        File target=new File(c.getExternalFilesDir("voice-downloads"),archive(kind));target.delete();
        DownloadManager.Request r=new DownloadManager.Request(Uri.parse(RELEASE+archive(kind))).setTitle("Milo · AI voice").setDescription(size(kind)+" · runs offline after download")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE).setAllowedOverMetered(false).setDestinationInExternalFilesDir(c,"voice-downloads",archive(kind));
        c.getSharedPreferences("milo",Context.MODE_PRIVATE).edit().putLong("voice_download_"+kind,c.getSystemService(DownloadManager.class).enqueue(r)).apply();
    }
    /** Unpacks a finished download, keeping only the files Milo uses. Runs on a background thread. */
    static boolean unpack(Context c,String kind){
        File archive=new File(c.getExternalFilesDir("voice-downloads"),archive(kind));forget(c,kind);if(!archive.isFile())return false;
        File out=dir(c,kind);out.mkdirs();String rootCanon;
        try{rootCanon=root(c).getCanonicalPath();}catch(IOException e){return false;}
        try(TarArchiveInputStream tar=new TarArchiveInputStream(new BZip2CompressorInputStream(new BufferedInputStream(new FileInputStream(archive))))){
            TarArchiveEntry e;byte[] buf=new byte[1<<16];
            while((e=tar.getNextEntry())!=null){
                String name=e.getName();int slash=name.indexOf('/');if(slash<0||e.isDirectory())continue;String rel=name.substring(slash+1);
                File dest;
                if(rel.startsWith("espeak-ng-data/"))dest=new File(root(c),rel);
                else if(rel.equals("model.int8.onnx")||rel.equals("voices.bin")||rel.equals("tokens.txt")||rel.equals("lexicon-us-en.txt")||rel.equals("ml_IN-meera-medium.onnx")||rel.equals("LICENSE")||rel.equals("MODEL_CARD"))dest=new File(out,rel);
                else continue;
                if(!dest.getCanonicalPath().startsWith(rootCanon+File.separator))continue;
                dest.getParentFile().mkdirs();try(OutputStream o=new FileOutputStream(dest)){int n;while((n=tar.read(buf))>0)o.write(buf,0,n);}
            }
        }catch(IOException e){Log.w(TAG,"unpack failed: "+e);return false;}
        archive.delete();return installed(c,kind);
    }
    /** Clears the tracked download so a failed or finished one is not unpacked again. */
    static void forget(Context c,String kind){c.getSharedPreferences("milo",Context.MODE_PRIVATE).edit().remove("voice_download_"+kind).apply();}
    static void delete(Context c,String kind){forget(c,kind);release();deleteTree(dir(c,kind));if(!installed(c,KOKORO)&&!installed(c,MEERA))deleteTree(espeak(c));}
    private static void deleteTree(File f){File[] kids=f.listFiles();if(kids!=null)for(File k:kids)deleteTree(k);f.delete();}
}
