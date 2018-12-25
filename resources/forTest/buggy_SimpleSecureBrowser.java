package com.dalthed.tucan.Connection;

import com.dalthed.tucan.R;

import com.dalthed.tucan.ui.SimpleWebActivity;
import com.dalthed.tucan.ui.SimpleWebListActivity;


import android.app.ProgressDialog;

import android.os.AsyncTask;
/**
 * SimpleSecureBrowser ist ein AsyncTask welcher die RequestObjects passend abschickt und zur�ckgibt.
 * Muss aus einer SimpleWebListActivity gestartet werden. Nachdem die Daten angekommen sind, wird
 * die onPostExecute der aufrufenden SimpleWebListActivity aufgerufen.
 * @author Tyde
 *
 */
public class SimpleSecureBrowser extends AsyncTask<RequestObject, Integer, AnswerObject> {
    protected SimpleWebListActivity outerCallingListActivity;
    protected SimpleWebActivity outerCallingActivity;
    ProgressDialog dialog;

    /**
     * Die Activity muss �bergeben werden, damit der Browser die Methode onPostExecute aufrufen kann
     * @param callingActivity
     */
    public SimpleSecureBrowser (SimpleWebListActivity callingActivity) {
        super();
        outerCallingListActivity=callingActivity;
        outerCallingActivity=null;
    }

    public SimpleSecureBrowser (SimpleWebActivity callingActivity) {
        outerCallingListActivity=null;
        outerCallingActivity=callingActivity;
    }
    @Override
    protected AnswerObject doInBackground(RequestObject... requestInfo) {
        AnswerObject answer = new AnswerObject("", "", null,null);
        RequestObject significantRequest = requestInfo[0];
        BrowseMethods Browser=new BrowseMethods();
        answer=Browser.browse(significantRequest);
        return answer;
    }

    @Override
    protected void onPreExecute() {
        if(outerCallingListActivity==null){
            dialog = ProgressDialog.show(outerCallingActivity,"",
                    outerCallingActivity.getResources().getString(R.string.ui_load_data),true);
        }
        else {
            dialog = ProgressDialog.show(outerCallingListActivity,"",
                    outerCallingListActivity.getResources().getString(R.string.ui_load_data),true);
        }
    }

    @Override
    protected void onPostExecute(AnswerObject result) {
        if(outerCallingListActivity==null){
            dialog.setTitle(outerCallingActivity.getResources().getString(R.string.ui_calc));
            outerCallingActivity.onPostExecute(result);
        }
        else {
            dialog.setTitle(outerCallingListActivity.getResources().getString(R.string.ui_calc));
            outerCallingListActivity.onPostExecute(result);
        }
        dialog.dismiss();
    }


}