package com.dummy.fooforandroid;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.googlecode.android_scripting.AndroidProxy;
import com.googlecode.android_scripting.Constants;
import com.googlecode.android_scripting.FeaturedInterpreters;
import com.googlecode.android_scripting.FileUtils;
import com.googlecode.android_scripting.Log;
import com.googlecode.android_scripting.ScriptLauncher;
import com.googlecode.android_scripting.interpreter.Interpreter;
import com.googlecode.android_scripting.interpreter.InterpreterConfiguration;
import com.googlecode.android_scripting.interpreter.InterpreterUtils;

import java.io.File;

public class ScriptService extends Service {
  private final IBinder mBinder;

  public class LocalBinder extends Binder {
    public ScriptService getService() {
      return ScriptService.this;
    }
  }

  public ScriptService() {
    mBinder = new LocalBinder();
  }

  @Override
  public void onStart(Intent intent, final int startId) {
    super.onStart(intent, startId);
    ScriptApplication app = (ScriptApplication) getApplication();
    InterpreterConfiguration config = app.getInterpreterConfiguration();
    String fileName = Script.getFileName(this);
    Interpreter interpreter = config.getInterpreterForScript(fileName);

    if (interpreter == null || !interpreter.isInstalled()) {
      if (FeaturedInterpreters.isSupported(fileName)) {
        Intent i = new Intent(this, DialogActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.putExtra(Constants.EXTRA_SCRIPT_NAME, fileName);
        startActivity(i);
      } else {
        Log.e(this, "Cannot find an interpreter for script " + fileName);
      }
      stopSelf(startId);
      return;
    }

    // Copies script to internal memory.
    fileName = InterpreterUtils.getInterpreterRoot(this).getAbsolutePath() + "/" + fileName;
    File script = new File(fileName);
    if (!script.exists()) {
      script = FileUtils.copyFromStream(fileName, getResources().openRawResource(Script.ID));
    }

    final AndroidProxy proxy = new AndroidProxy(this, null, true);
    proxy.startLocal();
    ScriptLauncher.launchScript(proxy, script, null, new Runnable() {
      @Override
      public void run() {
        proxy.shutdown();
        stopSelf(startId);
      }
    });
  }

  @Override
  public IBinder onBind(Intent intent) {
    return mBinder;
  }
}
