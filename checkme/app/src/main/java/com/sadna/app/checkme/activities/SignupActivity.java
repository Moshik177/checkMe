package com.sadna.app.checkme.activities;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.gson.Gson;
import com.sadna.app.checkme.BaseActivity;
import com.sadna.app.checkme.MyApplication;
import com.sadna.app.checkme.R;
import com.sadna.app.checkme.forms.SignUpForm;
import com.sadna.app.webservice.WebService;

import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

public class SignupActivity extends BaseActivity {

    private static final int USER_ALREADY_EXISTS = 1;
    private Calendar mCalendar = Calendar.getInstance();
    private SignUpTask mSignUpTask = null;
    private static int mSignUpActionResult;
    private static final int RSUALT_LOAD_IMAGE = 2;
    public static String base64Picture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        TextView birthdateTextView = (TextView) findViewById(R.id.birthdayText);
        Context context = getApplicationContext();
        birthdateTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                birthDatePicker(v);
            }
        });


        //fonts
        Typeface face=Typeface.createFromAsset(getAssets(),"fonts/gishabd.ttf");
        TextView toolbarText = (TextView) findViewById(R.id.toolbarText);
        toolbarText.setTypeface(face);

        RadioButton femaleButton = (RadioButton) findViewById(R.id.femaleButton);
        femaleButton.setTypeface(face);

        RadioButton maleButton = (RadioButton) findViewById(R.id.maleButton);
        maleButton.setTypeface(face);

        EditText password = (EditText) findViewById(R.id.passwordText);
        password.setTypeface(face);

        EditText username = (EditText) findViewById(R.id.userNameText);
        username.setTypeface(face);

        EditText birthdayText = (EditText) findViewById(R.id.birthdayText);
        birthdayText.setTypeface(face);

        EditText emailText = (EditText) findViewById(R.id.emailText);
        emailText.setTypeface(face);

        EditText lastNameText = (EditText) findViewById(R.id.lastNameText);
        lastNameText.setTypeface(face);

        EditText firstNameText = (EditText) findViewById(R.id.firstNameText);
        firstNameText.setTypeface(face);

        Button signUpButton = (Button) findViewById(R.id.signUpButton);
        signUpButton.setTypeface(face);

    }

    public void uploadPhoto(View view) {

        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent,RSUALT_LOAD_IMAGE);
        Button addPicture = (Button) findViewById(R.id.addPicture);
        addPicture.setText("התמונה נוספה");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
       super.onActivityResult(requestCode,requestCode,data);
        /// / Check which request we're responding to
        if (requestCode == RSUALT_LOAD_IMAGE && resultCode == RESULT_OK && data !=null ) {
            try {
                // get uri from Intent
                Uri uri = data.getData();
                // get bitmap from uri
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                // store bitmap to file
                File filename = new File(Environment.getExternalStorageDirectory(), "imageName.jpg");
                FileOutputStream out = new FileOutputStream(filename);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 60, out);
                out.flush();
                out.close();
                // get base64 string from file
                base64Picture = getStringImage(filename);
                // use base64 for your next step.
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String getStringImage(File file){
        try {
            FileInputStream fin = new FileInputStream(file);
            byte[] imageBytes = new byte[(int)file.length()];
            fin.read(imageBytes, 0, imageBytes.length);
            fin.close();
            return Base64.encodeToString(imageBytes, Base64.DEFAULT);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private static boolean uploadPhotoToServer(String base64Picture, int number,int id){

        WebService wsHttpRequest = new WebService("setPhoto");
        String result = null;
        try {
            result = wsHttpRequest.execute(base64Picture,Integer.toString(number),Integer.toString(id));
        } catch (Throwable exception) {
            exception.printStackTrace();
            return false;
        }
        return true;
    }

    public void attemptSignUp(View view) {
        SignUpForm signUpForm = new SignUpForm();
        signUpForm.setFirstName(((EditText) findViewById(R.id.firstNameText)).getText().toString());
        signUpForm.setLastName(((EditText) findViewById(R.id.lastNameText)).getText().toString());
        signUpForm.setEmail(((EditText) findViewById(R.id.emailText)).getText().toString());
        signUpForm.setUsername(((EditText) findViewById(R.id.userNameText)).getText().toString());
        signUpForm.setPassword(((EditText) findViewById(R.id.passwordText)).getText().toString());
        signUpForm.setBirthdate(((EditText) findViewById(R.id.birthdayText)).getText().toString());
        signUpForm.setGender(getGenderFromSignUpForm());
        signUpForm.setPhone(getApplicationContext().getSharedPreferences("CheckMePref", 0).getString("verify_phoneNumber", ""));
        ((MyApplication)getApplication()).setUsername(signUpForm.getUsername());
        if (!validateForm(signUpForm)) {
            return;
        }

        // Actually try to use WS to add user. Check for exception if username already exists
        // If all good, move to login activity

        mSignUpTask = new SignUpTask(signUpForm);
        mSignUpTask.execute((Void) null);
    }

    private boolean validateForm(SignUpForm signUpForm) {
        boolean valid = true;

        resetErrors();

        if (TextUtils.isEmpty(signUpForm.getFirstName())) {
            ((EditText) findViewById(R.id.firstNameText)).setError(getString(R.string.error_field_required));
            valid = false;
        }

        if (TextUtils.isEmpty(signUpForm.getLastName())) {
            ((EditText) findViewById(R.id.lastNameText)).setError(getString(R.string.error_field_required));
            valid = false;
        }

        if (TextUtils.isEmpty(signUpForm.getEmail())) {
            ((EditText) findViewById(R.id.emailText)).setError(getString(R.string.error_field_required));
            valid = false;
        } else if (!validateEmail(signUpForm.getEmail())) {
            ((EditText) findViewById(R.id.emailText)).setError(getString(R.string.error_invalid_email));
            valid = false;
        }

        if (TextUtils.isEmpty(signUpForm.getUsername())) {
            ((EditText) findViewById(R.id.userNameText)).setError(getString(R.string.error_field_required));
            valid = false;
        }

        if (TextUtils.isEmpty(signUpForm.getPassword())) {
            ((EditText) findViewById(R.id.passwordText)).setError(getString(R.string.error_field_required));
            valid = false;
        }

        if (TextUtils.isEmpty(signUpForm.getBirthdate())) {
            ((EditText) findViewById(R.id.birthdayText)).setError(getString(R.string.error_field_required));
            valid = false;
        }

        if (signUpForm.getGender() == null) {
            ((RadioButton) findViewById(R.id.femaleButton)).setError(getString(R.string.error_field_required));
            valid = false;
        }

        return valid;
    }

    private void resetErrors() {
        ((EditText) findViewById(R.id.firstNameText)).setError(null);
        ((EditText) findViewById(R.id.lastNameText)).setError(null);
        ((EditText) findViewById(R.id.emailText)).setError(null);
        ((EditText) findViewById(R.id.userNameText)).setError(null);
        ((EditText) findViewById(R.id.passwordText)).setError(null);
        ((EditText) findViewById(R.id.birthdayText)).setError(null);
        ((RadioButton) findViewById(R.id.femaleButton)).setError(null);
    }

    public void resetGenderErrors(View view) {
        ((RadioButton) findViewById(R.id.femaleButton)).setError(null);
    }

    public void birthDatePicker(View view) {
        DatePickerDialog.OnDateSetListener date = new DatePickerDialog.OnDateSetListener() {

            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear,
                                  int dayOfMonth) {
                // TODO Auto-generated method stub
                mCalendar.set(Calendar.YEAR, year);
                mCalendar.set(Calendar.MONTH, monthOfYear);
                mCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                updateTextBox();
            }
        };

        DatePickerDialog bdDialog = new DatePickerDialog(SignupActivity.this, date, mCalendar
                .get(Calendar.YEAR), mCalendar.get(Calendar.MONTH),
                mCalendar.get(Calendar.DAY_OF_MONTH));

        bdDialog.getDatePicker().setCalendarViewShown(false);
        bdDialog.getDatePicker().setSpinnersShown(true);
        bdDialog.getDatePicker().setMaxDate(new Date().getTime());
        bdDialog.setTitle("מתי יום הולדתך?");
        bdDialog.show();
    }

    private void updateTextBox() {
        SimpleDateFormat formatDate = new SimpleDateFormat("dd/MM/yyyy");
        ((EditText) findViewById(R.id.birthdayText)).setError(null);
        ((EditText) findViewById(R.id.birthdayText)).setText(formatDate.format(mCalendar.getTime()));
    }

    private boolean validateEmail(String email) {
        boolean result = true;
        try {
            InternetAddress emailAddr = new InternetAddress(email);
            emailAddr.validate();
        } catch (AddressException ex) {
            result = false;
        }
        return result;
    }

    private String getGenderFromSignUpForm() {

        RadioGroup rg =  ((RadioGroup) findViewById(R.id.genderGroup));

        if (((RadioGroup) findViewById(R.id.genderGroup)).getCheckedRadioButtonId() == -1) {
            return null;
        }

        String radiovalue = ((RadioButton)findViewById(rg.getCheckedRadioButtonId())).getText().toString();

        if (radiovalue.equals("זכר")) {
            return "Male";
        }

        return "Female";
    }



    public static boolean signUpUser(String firstname, String lastname, String email, String username, String password, String birthdate, String gender, String phone) {
        WebService wsHttpRequest = new WebService("addUser");
        String result = null;

        try {
            result = wsHttpRequest.execute(firstname, lastname, email, username, password, birthdate, gender, phone);
            JSONObject userDetailsJsonObject = new JSONObject(result);
            uploadPhotoToServer(base64Picture, RSUALT_LOAD_IMAGE,userDetailsJsonObject.getInt("id"));
            FirebaseInstanceId.getInstance().getToken();
        } catch (Throwable exception) {
            if (exception.getMessage().contains("User already exists")) {
                mSignUpActionResult = USER_ALREADY_EXISTS;
            }
            exception.printStackTrace();
            return false;
        }

        return true;
    }

    public class SignUpTask extends AsyncTask<Void, Void, Boolean> {

        private SignUpForm mSignUpForm;
        private AlertDialog.Builder mBuilder = null;

        SignUpTask(SignUpForm signUpForm) {
            mSignUpForm = signUpForm;

        }

        @Override
        protected Boolean doInBackground(Void... params) {

            return signUpUser(mSignUpForm.getFirstName(), mSignUpForm.getLastName(), mSignUpForm.getEmail(),
                    mSignUpForm.getUsername(), mSignUpForm.getPassword(), mSignUpForm.getBirthdate(), mSignUpForm.getGender(), mSignUpForm.getPhone());
        }

        protected void onPreExecute() {
            super.onPreExecute();
            mBuilder = new AlertDialog.Builder(SignupActivity.this);
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mSignUpForm = null;

            if (success) {
                TextView title = new TextView(SignupActivity.this);
                title.setText("ההרשמה הושלמה!");
                title.setGravity(Gravity.RIGHT);
                title.setPadding(10, 10, 10, 10);
                title.setTextColor(Color.BLACK);
                title.setTextSize(20);
                mBuilder.setCustomTitle(title);
                mBuilder.setCustomTitle(title)
                        .setMessage("תהליך ההרשמה הושלם לחץ כאן כדי לעבור לעמוד ההתחברות")
                        .setCancelable(false)
                        .setPositiveButton("אישור", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                finish();
                                startActivity(new Intent(getApplicationContext(), LoginActivity.class));

                            }
                        });
                AlertDialog alert = mBuilder.create();
                alert.show();
            } else {
                executeResult();
            }
        }

        private void executeResult() {
            switch (mSignUpActionResult) {
                case USER_ALREADY_EXISTS:
                    ((EditText) findViewById(R.id.userNameText)).setError(getString(R.string.error_user_already_exists));
                    findViewById(R.id.userNameText).requestFocus();
                    break;
                default:
                    findViewById(R.id.firstNameText).requestFocus();
                    break;
            }
        }

        @Override
        protected void onCancelled() {
            mSignUpForm = null;
        }
    }
}








