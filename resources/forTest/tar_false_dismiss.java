package com.kivsw.mvprxdialog;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * Use the {@link MvpInputBox#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MvpInputBox extends BaseMvpFragment {

    private View rootView;
    private Button okBtn,cancelBtn;
    private TextView textView;
    private EditText editText;

    private final static String
            MESSAGE_PARAM="MESSAGE_PARAM",
            INPUT_TYPE_PARAM="OK_TITLE_PARAM",
            OLD_VALUE_PARAM = "OLD_VALUE_PARAM";


    public static MvpInputBox newInstance(long presenterId, Bitmap icon, String title, String msg, String InputValue, int inputType)
    {
        MvpInputBox fragment = new MvpInputBox();
        Bundle args = new Bundle();

        args.putLong(PRESENTER_ID, presenterId);
        args.putParcelable(ICON_PARAM,icon);
        args.putString(MESSAGE_PARAM,msg);
        args.putString(TITLE_PARAM,title);
        args.putString(OLD_VALUE_PARAM, InputValue);
        args.putInt(INPUT_TYPE_PARAM,inputType);

        fragment.setArguments(args);
        return fragment;
    };

    public MvpInputBox() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.mvp_inputbox, container, false);

        setupTitle(rootView);

        okBtn= (Button)rootView.findViewById(R.id.dlButtonOk);
        okBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MvpInputBoxPresenter)getPresenter()).onOkPress(editText.getText());
            }
        });
        okBtn.setText(android.R.string.ok);
        cancelBtn=(Button)rootView.findViewById(R.id.dlButtonCancel);
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MvpInputBoxPresenter)getPresenter()).onCancelPress();
            }
        });
        cancelBtn.setText(android.R.string.cancel);

        editText=(EditText)rootView.findViewById(R.id.dlEditValue);

        editText.setInputType(this.getArguments().getInt(INPUT_TYPE_PARAM));
        if(savedInstanceState==null)
            editText.setText(this.getArguments().getString(OLD_VALUE_PARAM));
        editText.requestFocus();
        //editText.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, 0, 0, 0));
        //editText.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, 0, 0, 0));
        //getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        textView=(TextView)rootView.findViewById(R.id.dlInputValTextView);
        textView.setText(this.getArguments().getString(MESSAGE_PARAM));

        return rootView;
    }



    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    public void showErrorMessage(CharSequence errMessage)
    {
        editText.setError(errMessage);
    }

}
