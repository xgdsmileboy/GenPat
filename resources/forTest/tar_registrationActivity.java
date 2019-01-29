package com.sqbnet.expressassistant;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.sqbnet.expressassistant.Provider.SQBProvider;
import com.sqbnet.expressassistant.controls.CustomSelectionPopUp;
import com.sqbnet.expressassistant.mode.SQBResponse;
import com.sqbnet.expressassistant.mode.SQBResponseListener;
import com.sqbnet.expressassistant.utils.UtilHelper;

import com.sqbnet.expressassistant.Provider.SQBProvider;
import com.sqbnet.expressassistant.mode.SQBResponse;
import com.sqbnet.expressassistant.mode.SQBResponseListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Time;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.jar.JarException;


public class registrationActivity extends BaseActivity {

    private Button btn_ok;
    private Button btn_cancel;
    private Button btn_pick_photo;
    private Button btn_get_passcode;

    private TextView tv_agreement;
    private TextView tv_photo_placeholder;
    private CheckBox chkbox_accept_protocol;

    private EditText et_username;
    private EditText et_password;
    private EditText et_real_name;
    private EditText et_id;
    private int et_photo_id;
    private EditText et_mobile;
    private EditText et_passcode;
    private EditText et_addr;

    private Button sp_province;
    private Button sp_city;
    private Button sp_district;
    private Button sp_gender;

    private Bitmap photo;
    private String photoPath;
    private String phoneCode;

    private TimeCount timeCount;

    private List<Map<String, Object>> mProvinces;
    private List<Map<String, Object>> mCities;
    private List<Map<String, Object>> mDistricts;
    private List<Map<String, Object>> mGenders;
    private Map<String, Object> mSelectedProvince;
    private Map<String, Object> mSelectedCity;
    private Map<String, Object> mSelectedDistrict;
    private Map<String, Object> mSelectedGender;
    private Map<Integer, List<Integer>> mDistrictCache;
    private Map<Integer, String> mDistrictMapCache;

    private CustomSelectionPopUp mProvincePopup;
    private CustomSelectionPopUp mCityPopup;
    private CustomSelectionPopUp mDistrictPopup;
    private CustomSelectionPopUp mGenderPopup;


    private SimpleAdapter provinceAdapter;
    private SimpleAdapter cityAdapter;
    private SimpleAdapter districtAdapter;

    private Map<String, Object> mEmptyItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        timeCount = new TimeCount(60000, 1000);
        initView();
    }

    private void initView() {
        initView_Buttons();
        initView_EditText();
        initView_Others();
    }

    private void initView_Buttons() {
        btn_ok = (Button) findViewById(R.id.btn_registration_ok);
        btn_cancel = (Button) findViewById(R.id.btn_registration_cancel);
        btn_pick_photo = (Button) findViewById(R.id.btn_registration_pick_photo);
        btn_get_passcode = (Button) findViewById(R.id.btn_registration_get_passcode);

        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (et_username.length() == 0) {
                    showToast("账号不能为空");
                    return;
                }
                if (et_password.length() == 0) {
                    showToast("密码不能为空");
                    return;
                }
                if (et_real_name.length() == 0) {
                    showToast("真实姓名不能为空");
                    return;
                }
                if (mSelectedGender == null) {
                    showToast("性别不能为空");
                    return;
                }
                if (et_id.length() == 0) {
                    showToast("身份证号不能为空");
                    return;
                }
                try {
                    if (!UtilHelper.IDCardValidate(et_id.getText().toString()).equals("")) {
                        showToast("身份证号码不正确");
                        return;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (photoPath == null) {
                    showToast("身份证照片不能为空");
                    return;
                }
                if (et_mobile.length() == 0) {
                    showToast("手机号码不能为空");
                }
                if (et_passcode.length() == 0) {
                    showToast("验证码不能为空");
                    return;
                }
                if (!et_passcode.getText().toString().equals(phoneCode)) {
                    showToast("验证码不正确");
                    return;
                }
                if (et_addr.length() == 0) {
                    showToast("地址不能为空");
                    return;
                }
                if (mProvinces.size() > 1 && mSelectedProvince == null) {
                    showToast("省份不能为空");
                    return;
                }
                if (mCities.size() > 1 && mSelectedCity == null) {
                    showToast("城市不能为空");
                    return;
                }
                if (mDistricts.size() > 1 && mSelectedDistrict == null) {
                    showToast("地区不能为空");
                    return;
                }


                final ProgressDialog progressDialog = UtilHelper.getProgressDialog("正在上传身份证照片，请稍候...", registrationActivity.this);
                progressDialog.show();
                progressDialog.setCancelable(false);
                SQBProvider.getInst().uploadPhoto(photoPath, new SQBResponseListener() {
                    @Override
                    public void onResponse(final SQBResponse response) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (response == null) {
                                    showToast("上传身份证照片出现错误，请重试");
                                    return;
                                }
                                Log.i("virgil", response.getCode());
                                Log.i("virgil", response.getMsg());
                                Log.i("virgil", response.getData().toString());
                                if (response.getCode().equals("1000")) {
                                    try {
                                        final String photoID = ((JSONObject) response.getData()).getString("id");

                                        progressDialog.setMessage("身份证上传成功，注册中...");
                                        String userName = et_username.getText().toString();
                                        String password = et_password.getText().toString();
                                        password = UtilHelper.MD5(UtilHelper.MD5(password) + phoneCode);
                                        final String realName = et_real_name.getText().toString();
                                        String idCard = et_id.getText().toString();
                                        String phone = et_mobile.getText().toString();
                                        String addr = et_addr.getText().toString();
                                        String province = mSelectedProvince != null ? mSelectedProvince.get("id").toString() : "";
                                        String city = mSelectedCity != null ? mSelectedCity.get("id").toString() : "";
                                        String district = mSelectedDistrict != null ? mSelectedDistrict.get("id").toString() : "";
                                        String gender = mSelectedGender != null ? mSelectedGender.get("id").toString() : "";

                                        Log.i("virgil", "province:" + province);
                                        Log.i("virgil", "city:" + city);
                                        Log.i("virgil", "district:" + district);
                                        SQBProvider.getInst().userRegister(userName, password, realName, idCard, photoID, phone, addr, phoneCode, province, city, district, gender, new SQBResponseListener() {
                                            @Override
                                            public void onResponse(final SQBResponse response) {
                                                if(response == null){
                                                    runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            if (progressDialog.isShowing()) {
                                                                progressDialog.dismiss();
                                                            }
                                                            showToast("服务器异常，请稍候再试");
                                                        }
                                                    });
                                                    return;
                                                }
                                                Log.i("virgil", response.getCode());
                                                Log.i("virgil", response.getMsg());
                                                Log.i("virgil", response.getData().toString());
                                                if (response.getCode().equals("1000")) {
                                                    runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            if (progressDialog.isShowing()) {
                                                                progressDialog.dismiss();
                                                            }
                                                            Intent intent = new Intent(registrationActivity.this, registrationSuccessActivity.class);
                                                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                                            startActivity(intent);
                                                        }
                                                    });
                                                } else {
                                                    runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            if (progressDialog.isShowing()) {
                                                                progressDialog.dismiss();
                                                            }
                                                            showToast(response.getMsg());
                                                        }
                                                    });
                                                }
                                            }
                                        });
                                    } catch (Exception e) {
                                        if (progressDialog.isShowing()) {
                                            progressDialog.dismiss();
                                        }
                                        e.printStackTrace();
                                    }
                                } else {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (progressDialog.isShowing()) {
                                                progressDialog.dismiss();
                                            }
                                            showToast(response.getMsg());
                                        }
                                    });
                                }
                            }
                        });
                    }
                });
            }
        });

        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*Intent intent = new Intent();
                intent.setClass(registrationActivity.this, loginActivity.class);

                startActivity(intent);
                overridePendingTransition(R.animator.in_from_left, R.animator.out_to_right);*/
                finish();
            }
        });

        btn_pick_photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent getIntent = new Intent(Intent.ACTION_GET_CONTENT);
                getIntent.setType("image/*");

                Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                pickIntent.setType("image/*");

                Intent chooserIntent = Intent.createChooser(getIntent, getResources().getString(R.string.registration_pick_photo));
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{pickIntent});

                startActivityForResult(chooserIntent, RequestCode.PICK_PHOTO);
            }
        });

        btn_get_passcode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (et_mobile.getText() == null || et_mobile.length() == 0) {
                    return;
                }
                String mobile = et_mobile.getText().toString();
                Log.i("virgil", mobile);
                if (!UtilHelper.isMobileNO(mobile)) {
                    Log.i("virgil", "not valid mobile");
                    Toast.makeText(getApplicationContext(), "手机号码格式不正确，请重新填写", Toast.LENGTH_SHORT).show();
                    return;
                }
                SQBProvider.getInst().sendSMS(mobile, new SQBResponseListener() {
                    @Override
                    public void onResponse(final SQBResponse response) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (response == null) {
                                    showToast("注册出现错误，请重试");
                                    return;
                                }
                                Log.i("virgil", response.getCode());
                                Log.i("virgil", response.getMsg());
                                Log.i("virgil", response.getData().toString());
                                Toast.makeText(getApplicationContext(), response.getMsg(), Toast.LENGTH_SHORT).show();
                                if (response.getCode().equals("1000")) {
                                    try {
                                        phoneCode = ((JSONObject) response.getData()).getString("phone_code");
                                        Log.i("virgil", phoneCode);
                                        timeCount.start();
                                    } catch (JSONException e) {
                                        Toast.makeText(getApplicationContext(), "发送验证码失败，请稍后再试", Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    timeCount.onFinish();
                                }
                            }
                        });
                    }
                });
            }
        });
    }

    class TimeCount extends CountDownTimer {
        public TimeCount(long millsInFuture, long countDownInterval){
            super(millsInFuture, countDownInterval);
        }

        @Override
        public void onFinish() {
            btn_get_passcode.setText("重发验证码");
            btn_get_passcode.setClickable(true);
        }

        @Override
        public void onTick(long l) {
            btn_get_passcode.setClickable(false);
            btn_get_passcode.setText(l/1000 + "秒");
        }
    }

    private void initView_EditText() {
        et_username = (EditText) findViewById(R.id.et_registration_username);
        et_password = (EditText) findViewById(R.id.et_registration_password);
        et_real_name = (EditText) findViewById(R.id.et_registration_real_name);
        et_id = (EditText) findViewById(R.id.et_registration_id);
        et_mobile = (EditText) findViewById(R.id.et_registration_mobile);
        et_passcode = (EditText) findViewById(R.id.et_registration_passcode);
        et_addr = (EditText) findViewById(R.id.et_registration_full_addr);
    }

    @SuppressWarnings("unchecked")
    private void initView_Others() {
        tv_photo_placeholder = (TextView) findViewById(R.id.tv_photo_placeholder);
        tv_agreement = (TextView) findViewById(R.id.tv_registration_agreement);
        chkbox_accept_protocol = (CheckBox) findViewById(R.id.chkbox_registration_accept_protocol);
        chkbox_accept_protocol.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                btn_ok.setEnabled(b);
            }
        });

        tv_agreement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setClass(registrationActivity.this, registrationAgreementActivity.class);

                startActivity(intent);
            }
        });

        mDistrictCache = new HashMap<Integer, List<Integer>>();
        mDistrictMapCache = new HashMap<Integer, String>();
        mProvinces = new ArrayList<Map<String, Object>>();
        mCities = new ArrayList<Map<String, Object>>();
        mDistricts = new ArrayList<Map<String, Object>>();
        sp_province = (Button) findViewById(R.id.sp_registration_province);
        sp_city = (Button) findViewById(R.id.sp_registration_city);
        sp_district = (Button) findViewById(R.id.sp_registration_district);
        sp_gender = (Button) findViewById(R.id.sp_registration_gender);

        mGenders = new ArrayList<Map<String, Object>>();
        Map<String, Object> gender_male = new HashMap<String, Object>();
        gender_male.put("id", 1);
        gender_male.put("name", getResources().getString(R.string.gender_male));
        Map<String, Object> gender_female = new HashMap<String, Object>();
        gender_female.put("id", 2);
        gender_female.put("name", getResources().getString(R.string.gender_female));
        mGenders.add(gender_male);
        mGenders.add(gender_female);

        /*sp_province.setPromptId(R.string.registration_province);
        sp_city.setPromptId(R.string.registration_city);
        sp_district.setPromptId(R.string.registration_district);*/

        mEmptyItem = new HashMap<String, Object>();
        mEmptyItem.put("id", -1);
        mEmptyItem.put("name", "");

        /*provinceAdapter = new SimpleAdapter(this, mProvinces, android.R.layout.simple_spinner_item,
                new String[] {
                       "name"
                },
                new int[] {
                        android.R.id.text1
                });
        sp_province.setAdapter(provinceAdapter);*/

       /* cityAdapter = new SimpleAdapter(this, mCities, android.R.layout.simple_spinner_item,
                new String[] {
                        "name"
                },
                new int[] {
                        android.R.id.text1
                });
        sp_city.setAdapter(cityAdapter);

        districtAdapter = new SimpleAdapter(this, mDistricts, android.R.layout.simple_spinner_item,
                new String[] {
                        "name"
                },
                new int[] {
                        android.R.id.text1
                });
        sp_district.setAdapter(districtAdapter);*/

        //mProvinces.add(mEmptyItem);
        SQBProvider.getInst().getArea("1", new SQBResponseListener() {
            @Override
            public void onResponse(final SQBResponse response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (response != null) {
                            try {
                                JSONArray result = (JSONArray) response.getData();
                                int length = result.length();
                                List<Integer> leaves = new ArrayList<Integer>();
                                for (int i = 0; i < length; i++) {
                                    Map<String, Object> data = new HashMap<String, Object>();
                                    JSONObject obj = (JSONObject) result.get(i);
                                    int id = obj.getInt("id");
                                    String name = obj.getString("name");
                                    data.put("id", id);
                                    data.put("name", name);
                                    leaves.add(id);
                                    mProvinces.add(data);
                                    mDistrictMapCache.put(id, name);
                                }

                                mDistrictCache.put(1, leaves);
                                //provinceAdapter.notifyDataSetChanged();
                                sp_province.setText("");
                                sp_city.setText("");
                                sp_district.setText("");
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
            }
        });

        mProvincePopup = new CustomSelectionPopUp(registrationActivity.this, getResources().getString(R.string.registration_province), mProvinces, new CustomSelectionPopUp.ICustomSelectionPopUpSelected() {
            @Override
            public void selected(Map<String, Object> map) {
                if (map == null) {
                    mSelectedProvince = null;
                    sp_province.setText("");
                    return;
                }
                final Integer id = (Integer) map.get("id");
                sp_province.setText((String) map.get("name"));
                mSelectedProvince = map;
                mSelectedDistrict = null;
                mSelectedCity = null;
                if (id == -1) {
                    mSelectedProvince = null;
                    return;
                }

                mCities.clear();
                //mCities.add(mEmptyItem);

                sp_district.setText("");
                sp_city.setText("");
                if (mDistrictCache.containsKey(id)) {
                    List<Integer> leaves = mDistrictCache.get(id);
                    int length = leaves.size();
                    for (int index = 0; index < length; index++) {
                        Map<String, Object> data = new HashMap<String, Object>();
                        data.put("id", leaves.get(index));
                        data.put("name", mDistrictMapCache.get(leaves.get(index)));
                        mCities.add(data);
                    }
                    //cityAdapter.notifyDataSetChanged();
                    mDistricts.clear();
                    //districtAdapter.notifyDataSetChanged();
                } else {
                    SQBProvider.getInst().getArea(id.toString(), new SQBResponseListener() {
                        @Override
                        public void onResponse(final SQBResponse response) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (response != null) {
                                        try {
                                            JSONArray result = (JSONArray) response.getData();
                                            int length = result.length();
                                            List<Integer> leaves = new ArrayList<Integer>();
                                            for (int i = 0; i < length; i++) {
                                                Map<String, Object> data = new HashMap<String, Object>();
                                                JSONObject obj = (JSONObject) result.get(i);
                                                int _id = obj.getInt("id");
                                                String name = obj.getString("name");
                                                data.put("id", _id);
                                                data.put("name", name);
                                                leaves.add(_id);
                                                mCities.add(data);
                                                mDistrictMapCache.put(_id, name);
                                            }
                                            mDistrictCache.put(id, leaves);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    //cityAdapter.notifyDataSetChanged();
                                    //mCityPopup.update();
                                    mDistricts.clear();
                                    //districtAdapter.notifyDataSetChanged();
                                    //mDistrictPopup.update();
                                }
                            });
                        }
                    });
                }
            }
        });

        sp_province.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                mProvincePopup.show();
            }
        });

        mCityPopup = new CustomSelectionPopUp(registrationActivity.this, getResources().getString(R.string.registration_city), mCities, new CustomSelectionPopUp.ICustomSelectionPopUpSelected() {
            @Override
            public void selected(Map<String, Object> map) {
                if (map == null) {
                    mSelectedCity = null;
                    sp_city.setText("");
                    return;
                }
                mSelectedCity = map;
                mSelectedDistrict = null;
                final Integer id = (Integer) map.get("id");
                sp_city.setText((String) map.get("name"));
                if (id == -1) {
                    mSelectedCity = null;
                    return;
                }

                mDistricts.clear();
                //mDistricts.add(mEmptyItem);
                sp_district.setText("");
                if (mDistrictCache.containsKey(id)) {
                    List<Integer> leaves = mDistrictCache.get(id);
                    int length = leaves.size();
                    for (int index = 0; index < length; index++) {
                        Map<String, Object> data = new HashMap<String, Object>();
                        data.put("id", leaves.get(index));
                        data.put("name", mDistrictMapCache.get(leaves.get(index)));
                        mDistricts.add(data);
                    }
                    //districtAdapter.notifyDataSetChanged();
                    //mDistrictPopup.update();
                } else {
                    SQBProvider.getInst().getArea(id.toString(), new SQBResponseListener() {
                        @Override
                        public void onResponse(final SQBResponse response) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (response != null) {
                                        try {
                                            JSONArray result = (JSONArray) response.getData();
                                            int length = result.length();
                                            List<Integer> leaves = new ArrayList<Integer>();
                                            for (int i = 0; i < length; i++) {
                                                Map<String, Object> data = new HashMap<String, Object>();
                                                JSONObject obj = (JSONObject) result.get(i);
                                                int _id = obj.getInt("id");
                                                String name = obj.getString("name");
                                                data.put("id", _id);
                                                data.put("name", name);
                                                leaves.add(_id);
                                                mDistricts.add(data);
                                                mDistrictMapCache.put(_id, name);
                                            }

                                            mDistrictCache.put(id, leaves);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    //districtAdapter.notifyDataSetChanged();
                                    //mDistrictPopup.update();
                                }
                            });
                        }
                    });
                }
            }
        });

        sp_city.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCityPopup.show();
            }
        });

        mDistrictPopup = new CustomSelectionPopUp(registrationActivity.this, getResources().getString(R.string.registration_district), mDistricts, new CustomSelectionPopUp.ICustomSelectionPopUpSelected() {
            @Override
            public void selected(Map<String, Object> map) {
                if (map == null) {
                    mSelectedDistrict = null;
                    sp_district.setText("");
                    return;
                }

                mSelectedDistrict = map;
                sp_district.setText((String) map.get("name"));
            }
        });

        sp_district.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mDistrictPopup.show();
            }
        });

        mGenderPopup = new CustomSelectionPopUp(registrationActivity.this, getResources().getString(R.string.registration_select_gender), mGenders, new CustomSelectionPopUp.ICustomSelectionPopUpSelected() {
            @Override
            public void selected(Map<String, Object> data) {
                if (data == null) {
                    mSelectedGender = null;
                    sp_gender.setText("");
                    return;
                }

                mSelectedGender = data;
                sp_gender.setText((String) data.get("name"));
            }
        });

        sp_gender.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mGenderPopup.show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            return;
        }

        if (requestCode == RequestCode.PICK_PHOTO) {
            try {
                Uri originalUri = data.getData();
                photo = MediaStore.Images.Media.getBitmap(getContentResolver(), originalUri);
                String fileName = getRealPathFromURI(originalUri);
                photoPath = fileName;
                int start = fileName.lastIndexOf("/");
                if (start != -1) {
                    fileName = fileName.substring(start + 1);
                }
                tv_photo_placeholder.setText(fileName);

            } catch (Exception e) {
                Log.e("registration", e.toString());
            }
        }
    }

    private String getRealPathFromURI(Uri contentUri) {
        String[] proj = { MediaStore.Images.Media.DATA };
        CursorLoader loader = new CursorLoader(registrationActivity.this, contentUri, proj, null, null, null);
        Cursor cursor = loader.loadInBackground();
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    private boolean validateEditText() {
        //TODO: validate all the edittext
        return true;
    }

    private void showToast(String message){
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (this.getCurrentFocus() != null) {
            clearFocus(et_username);
            clearFocus(et_password);
            clearFocus(et_real_name);
            clearFocus(et_id);
            clearFocus(et_addr);
            clearFocus(et_mobile);
            clearFocus(et_passcode);

            InputMethodManager mInputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            return mInputMethodManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), 0);
        }
        return super.onTouchEvent(event);
    }

    private void clearFocus(EditText et) {
        if (et != null && et.hasFocus()) {
            et.clearFocus();
        }
    }
}
