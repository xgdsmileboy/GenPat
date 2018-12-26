package com.sqbnet.expressassistant.controls;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.sqbnet.expressassistant.R;

import java.util.List;
import java.util.Map;

/**
 * Created by Andy on 7/19/2015.
 */
public class CustomSelectionPopUp {
    public interface ICustomSelectionPopUpSelected {
        void selected(Map<String, Object> data);
    }

    private Activity mActivity;
    private ICustomSelectionPopUpSelected mCallback;
    private String mTitle;
    private List<Map<String, Object>> mData;
    private SimpleAdapter adapter;
    private AlertDialog alertDialog;

    public void show() {
        alertDialog = new AlertDialog.Builder(mActivity).create();
        alertDialog.show();
        Window window = alertDialog.getWindow();
        window.setContentView(R.layout.customize_selection_pop_up);
        ListView lv = (ListView) window.findViewById(R.id.lv_customize_selection_pop_up);
        TextView tv = (TextView) window.findViewById(R.id.tv_customize_selection_pop_up_title);
        tv.setText(mTitle);
        adapter = new SimpleAdapter(mActivity, mData, R.layout.customize_selection_pop_up_list,
                new String[] {
                        "name"
                }, new int[] {
                R.id.tv_customize_selection_pop_up_text
        });
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (mCallback != null) {
                    mCallback.selected(mData.get(i));
                    alertDialog.dismiss();
                }
            }
        });
    }
}