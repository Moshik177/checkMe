package com.sadna.app.checkme.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.EditText;

import com.sadna.app.checkme.BaseActivity;
import com.sadna.app.checkme.R;

import java.util.Random;

public class PhoneVerificationSendActivity extends BaseActivity {

    private SharedPreferences mSharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_verification_send);
    }

    private void resetErrors() {
        ((EditText) findViewById(R.id.phone_number_textbox)).setError(null);
    }

    private boolean validateForm(String phoneNumber) {
        boolean valid = true;

        resetErrors();

        if (phoneNumber.isEmpty()) {
            ((EditText) findViewById(R.id.phone_number_textbox)).setError(getString(R.string.error_field_not_empty));
            valid = false;
        }

        else if (phoneNumber.length() != 10) {
            ((EditText) findViewById(R.id.phone_number_textbox)).setError(getString(R.string.error_phone_num_invalid));
            valid = false;
        }

        return valid;
    }

    public void sendSmsAndMoveToVerifyActivity(View view)
    {
        String phoneNumber = ((EditText) findViewById(R.id.phone_number_textbox)).getText().toString();

        if (!validateForm(phoneNumber)) {
            return;
        }

        mSharedPref = getApplicationContext().getSharedPreferences("CheckMePref", 0); // 0 - for private mode;
        String verificationCode = generateVerificationCode();
        String message = "הקוד אימות שלך הוא: " + verificationCode;

        SharedPreferences.Editor editor = mSharedPref.edit();
        editor.putString("verify_phoneNumber", phoneNumber);
        editor.putString("verify_code", verificationCode);
        editor.commit();
        sendSMS(PhoneNumberUtils.formatNumber(phoneNumber), message);
        finish();
        startActivity(new Intent(getApplicationContext(), PhoneVerificationVerifyActivity.class));
    }

    private void sendSMS(String phoneNumber, String message)
    {
        // An sms is being sent directly from the user's phone line due to lack of finance to use an external web service to send the SMS (cost money)
        // In the real world, we would use an external web service to send the SMS to the user instead of using the user's phone line.
        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(phoneNumber, null, message, null, null);
    }

    private String generateVerificationCode()
    {
        Random r = new Random( System.currentTimeMillis() );
        return Integer.toString(r.nextInt(10000) + 10000); // for getting 5 digits number
    }
}
