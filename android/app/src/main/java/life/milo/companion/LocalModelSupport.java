package life.milo.companion;
import android.os.*;
import com.google.mlkit.genai.prompt.Generation;
import com.google.mlkit.genai.prompt.GenerativeModel;
import com.google.mlkit.genai.prompt.java.GenerativeModelFutures;
import com.google.mlkit.genai.common.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/** Uses the SDK feature status rather than a phone-name or package heuristic. */
final class LocalModelSupport {
    enum State { AVAILABLE, DOWNLOADABLE, DOWNLOADING, UNAVAILABLE, ERROR }
    static final class Status {final State state;final String message;Status(State s,String m){state=s;message=m;}}
    static final ExecutorService worker=Executors.newSingleThreadExecutor();
    static final Handler ui=new Handler(Looper.getMainLooper());
    static Status status(int code){
        if(code==FeatureStatus.AVAILABLE)return new Status(State.AVAILABLE,"Local model ready. Open Talk to chat without an API key.");
        if(code==FeatureStatus.DOWNLOADABLE)return new Status(State.DOWNLOADABLE,"This phone supports local AI. Download the model once to start.");
        if(code==FeatureStatus.DOWNLOADING)return new Status(State.DOWNLOADING,"Model download in progress. Keep Wi-Fi connected and check again shortly.");
        return new Status(State.UNAVAILABLE,"Local AI is unavailable on this phone. Update Android AICore if supported, or choose an optional cloud provider. Offline Comfort, Goals and lessons still work.");
    }
    static void check(Consumer<Status> callback){worker.execute(()->{GenerativeModel model=null;try{model=Generation.INSTANCE.getClient();Status value=status(GenerativeModelFutures.from(model).checkStatus().get(30,TimeUnit.SECONDS));ui.post(()->callback.accept(value));}catch(Exception|LinkageError e){ui.post(()->callback.accept(new Status(State.ERROR,"Could not check local AI. Update AICore if your phone supports it, then try again.")));}finally{if(model!=null)model.close();}});}
    static void download(Consumer<Status> callback){worker.execute(()->{GenerativeModel model=null;try{model=Generation.INSTANCE.getClient();GenerativeModelFutures futures=GenerativeModelFutures.from(model);int code=futures.checkStatus().get(30,TimeUnit.SECONDS);if(code!=FeatureStatus.DOWNLOADABLE){Status value=status(code);ui.post(()->callback.accept(value));return;}ui.post(()->callback.accept(new Status(State.DOWNLOADING,"Downloading the local model through Android AICore…")));futures.download(new DownloadCallback(){public void onDownloadStarted(long bytes){}public void onDownloadProgress(long bytes){}public void onDownloadCompleted(){}public void onDownloadFailed(GenAiException e){}}).get(20,TimeUnit.MINUTES);Status value=status(futures.checkStatus().get(30,TimeUnit.SECONDS));ui.post(()->callback.accept(value));}catch(Exception|LinkageError e){ui.post(()->callback.accept(new Status(State.ERROR,"Model download did not finish. Check Wi-Fi and storage, then try again.")));}finally{if(model!=null)model.close();}});}
}
