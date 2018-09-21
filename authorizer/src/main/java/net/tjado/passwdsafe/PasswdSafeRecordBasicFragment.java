/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe;


import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Button;
import android.content.DialogInterface;

import net.tjado.passwdsafe.file.PasswdFileData;
import net.tjado.passwdsafe.file.PasswdHistory;
import net.tjado.passwdsafe.lib.PasswdSafeUtil;
import net.tjado.passwdsafe.lib.view.GuiUtils;
import net.tjado.passwdsafe.lib.ObjectHolder;
import net.tjado.passwdsafe.otp.AddActivity;
import net.tjado.passwdsafe.otp.ScanActivity;
import net.tjado.passwdsafe.otp.Token;
import net.tjado.passwdsafe.otp.TokenCode;
import net.tjado.passwdsafe.util.Pair;
import net.tjado.passwdsafe.view.CopyField;
import net.tjado.passwdsafe.view.PasswdLocation;
import net.tjado.passwdsafe.lib.view.TypefaceUtils;

import net.tjado.authorizer.OutputInterface;
import net.tjado.authorizer.OutputKeyboardAsRoot;

import org.pwsafe.lib.file.PwsRecord;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import static android.app.Activity.RESULT_OK;


/**
 * Fragment for showing basic fields of a password record
 */
public class PasswdSafeRecordBasicFragment
        extends AbstractPasswdSafeRecordFragment
        implements View.OnClickListener
{
    private static final String TAG = "PasswdSafeRecordBasicFragment";

    private boolean itsIsPasswordShown = false;
    private String itsHiddenPasswordStr;
    private String itsTitle;
    private View itsBaseRow;
    private TextView itsBaseLabel;
    private TextView itsBase;
    private View itsGroupRow;
    private TextView itsGroup;
    private View itsUserRow;
    private TextView itsUser;
    private View itsPasswordRow;
    private TextView itsPassword;
    private SeekBar itsPasswordSeek;
    private Button itsUsernameSend;
    private Button itsCredentialSend;
    private Button itsPasswordSend;
    private CheckBox itsSendReturnSuffix;
    private RadioGroup itsSendDelimiter;
    private View itsUrlRow;
    private TextView itsUrl;
    private View itsEmailRow;
    private TextView itsEmail;
    private View itsTimesRow;
    private View itsCreationTimeRow;
    private TextView itsCreationTime;
    private View itsLastModTimeRow;
    private TextView itsLastModTime;
    private View itsProtectedRow;
    private View itsReferencesRow;
    private ListView itsReferences;
    private Button itsOtpAdd;
    private Button itsOtpAddCamera;
    private TokenCode itsOtp;
    private TextView itsOtpCode;
    private ProgressBar itsOtpTimer;
    private View itsOtpTokenRow;

    private boolean itsOtpConfirmation;

    private String SUB_OTP;
    private String SUB_TAB;
    private String SUB_RETURN;
    private final int PERMISSIONS_REQUEST_CAMERA = 1;

    /**
     * Create a new instance of the fragment
     */
    public static PasswdSafeRecordBasicFragment newInstance(
            PasswdLocation location)
    {
        PasswdSafeRecordBasicFragment frag = new PasswdSafeRecordBasicFragment();
        frag.setArguments(createArgs(location));
        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        setHasOptionsMenu(true);
        View root = inflater.inflate(R.layout.fragment_passwdsafe_record_basic,
                                     container, false);
        itsBaseRow = root.findViewById(R.id.base_row);
        itsBaseRow.setOnClickListener(this);
        itsBaseLabel = (TextView)root.findViewById(R.id.base_label);
        itsBase = (TextView)root.findViewById(R.id.base);
        View baseBtn = root.findViewById(R.id.base_btn);
        baseBtn.setOnClickListener(this);
        itsGroupRow = root.findViewById(R.id.group_row);
        itsGroup = (TextView)root.findViewById(R.id.group);
        itsUserRow = root.findViewById(R.id.user_row);
        itsUser = (TextView)root.findViewById(R.id.user);
        itsPasswordRow = root.findViewById(R.id.password_row);
        itsPasswordRow.setOnClickListener(this);
        itsPassword = (TextView)root.findViewById(R.id.password);
        itsPasswordSeek = (SeekBar)root.findViewById(R.id.password_seek);
        itsPasswordSeek.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener()
                {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress,
                                                  boolean fromUser)
                    {
                        if (fromUser) {
                            updatePasswordShown(false, progress);
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar)
                    {
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar)
                    {
                    }
                });

        // username send
        itsUsernameSend = (Button)root.findViewById(R.id.username_send);
        itsUsernameSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences prefs = Preferences.getSharedPrefs(getContext());
                OutputInterface.Language lang;
                lang = Preferences.getUsbkbdLanguagePref(prefs);
                sendCredentialUsb(lang, true, false);
            }
        });
        itsUsernameSend.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {

                sendCredentialUsbCustomLang(true, false);
                return true;
            }
        });

        // credential send
        itsCredentialSend = (Button)root.findViewById(R.id.credential_send);
        itsCredentialSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences prefs = Preferences.getSharedPrefs(getContext());
                OutputInterface.Language lang;
                lang = Preferences.getUsbkbdLanguagePref(prefs);
                sendCredentialUsb(lang, true, true);
            }
        });
        itsCredentialSend.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {

                sendCredentialUsbCustomLang(true, true);
                return true;
            }
        });

        // password send
        itsPasswordSend = (Button)root.findViewById(R.id.password_send);
        itsPasswordSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences prefs = Preferences.getSharedPrefs(getContext());
                OutputInterface.Language lang;
                lang = Preferences.getUsbkbdLanguagePref(prefs);
                sendCredentialUsb(lang, false, true);
            }
        });
        itsPasswordSend.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {

                sendCredentialUsbCustomLang(false, true);
                return true;
            }
        });


        itsSendReturnSuffix = (CheckBox) root.findViewById(R.id.send_return_suffix);
        itsSendDelimiter = (RadioGroup) root.findViewById(R.id.send_delimiter);
        SharedPreferences prefs = Preferences.getSharedPrefs(getContext());
        if ( ! Preferences.getUsbkbdEnabled(prefs)) {
            itsUsernameSend.setVisibility(View.GONE);
            itsCredentialSend.setVisibility(View.GONE);
            itsPasswordSend.setVisibility(View.GONE);
            itsSendReturnSuffix.setVisibility(View.GONE);
            itsSendDelimiter.setVisibility(View.GONE);
        }

        itsUrlRow = root.findViewById(R.id.url_row);
        itsUrl = (TextView)root.findViewById(R.id.url);
        itsEmailRow = root.findViewById(R.id.email_row);
        itsEmail = (TextView)root.findViewById(R.id.email);
        itsTimesRow = root.findViewById(R.id.times_row);
        itsCreationTimeRow = root.findViewById(R.id.creation_time_row);
        itsCreationTime = (TextView)root.findViewById(R.id.creation_time);
        itsLastModTimeRow = root.findViewById(R.id.last_mod_time_row);
        itsLastModTime = (TextView)root.findViewById(R.id.last_mod_time);
        itsProtectedRow = root.findViewById(R.id.protected_row);
        itsReferencesRow = root.findViewById(R.id.references_row);
        itsReferences = (ListView)root.findViewById(R.id.references);
        itsReferences.setOnItemClickListener(
                new AdapterView.OnItemClickListener()
                {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view,
                                            int position, long id)
                    {
                        showRefRec(false, position);
                    }
                });

        registerForContextMenu(itsUserRow);
        registerForContextMenu(itsPasswordRow);


        itsOtpAdd = (Button)root.findViewById(R.id.otp_new);
        itsOtpAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startOtpAddActivity(true);
            }
        });

        itsOtpAddCamera = (Button)root.findViewById(R.id.otp_new_camera);
        itsOtpAddCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startOtpAddActivity(false);
            }
        });


        itsOtpCode = (TextView)root.findViewById(R.id.otp_code);

        itsOtpTimer = (ProgressBar)root.findViewById(R.id.otp_time);
        itsOtpTokenRow = root.findViewById(R.id.otp_token_row);

        itsOtpCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                generateOtpToken();
            }
        });

        SUB_OTP = getResources().getString(R.string.SUB_OTP);
        SUB_TAB = getResources().getString(R.string.SUB_TAB);
        SUB_RETURN = getResources().getString(R.string.SUB_RETURN);

        return root;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == 1) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                saveOtpChange(data.getExtras().getString("uri"), false);
                //PasswdSafeUtil.dbginfo("OTP", String.format("Store manual otp uri: %s", data.getExtras().getString("uri")));
            }
        } else if (requestCode == 2) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                saveOtpChange(data.getExtras().getString("uri"), false);
                //PasswdSafeUtil.dbginfo("OTP", String.format("Store camera otp uri: %s", data.getExtras().getString("uri")));
            }
        }
    }

    private void saveOtpChange(final String otpUri, final boolean counterUpdate) {
        final ObjectHolder<Pair<Boolean, PasswdLocation>> rc = new ObjectHolder<>();
        useRecordFile(new RecordFileUser()
        {
            @Override
            public void useFile(@Nullable RecordInfo info,
                                @NonNull PasswdFileData fileData)
            {

                PwsRecord record;
                boolean newRecord;
                if (info != null) {
                    record = info.itsRec;
                    newRecord = false;
                } else {
                    record = fileData.createRecord();
                    record.setLoaded();
                    newRecord = true;
                }

                if( fileData.isProtected(record)) {
                    return;
                }

                String oldOtp = fileData.getOtp(record);
                if (!otpUri.equals(oldOtp)) {
                    fileData.setOtp(otpUri, record);

                    PasswdHistory hist = fileData.getPasswdHistory(record);
                    if (oldOtp != null && !oldOtp.equals("") && hist != null && !counterUpdate) {
                        Date passwdDate = fileData.getPasswdLastModTime(record);
                        if (passwdDate == null) {
                            passwdDate = fileData.getCreationTime(record);
                        }

                        hist.addPasswd(oldOtp, passwdDate);
                        fileData.setPasswdHistory(hist, record, true);
                    }
                }

                if (newRecord) {
                    fileData.addRecord(record);
                }

                rc.set(new Pair<>((newRecord || record.isModified()), new PasswdLocation(record, fileData)));

            }
        });

        if (rc == null || rc.get() == null) {
            return;
        }
        getListener().finishEditRecord(rc.get().first, rc.get().second, false);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu)
    {
        MenuItem item = menu.findItem(R.id.menu_toggle_password);
        if (item != null) {
            item.setTitle(
                    itsIsPasswordShown ?
                            R.string.hide_password : R.string.show_password);
            item.setEnabled(itsPasswordRow.getVisibility() == View.VISIBLE);
        }

        item = menu.findItem(R.id.menu_copy_url);
        if (item != null) {
            item.setVisible(itsUrlRow.getVisibility() == View.VISIBLE);
        }

        item = menu.findItem(R.id.menu_copy_email);
        if (item != null) {
            item.setVisible(itsEmailRow.getVisibility() == View.VISIBLE);
        }

        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
        case R.id.menu_copy_user: {
            copyUser();
            return true;
        }
        case R.id.menu_copy_password: {
            copyPassword();
            return true;
        }
        case R.id.menu_copy_url: {
            copyUrl();
            return true;
        }
        case R.id.menu_copy_email: {
            copyEmail();
            return true;
        }
        case R.id.menu_toggle_password: {
            updatePasswordShown(true, 0);
            return true;
        }
        default: {
            return super.onOptionsItemSelected(item);
        }
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view,
                                    ContextMenu.ContextMenuInfo menuInfo)
    {
        super.onCreateContextMenu(menu, view, menuInfo);
        menu.setHeaderTitle(itsTitle);
        switch (view.getId()) {
        case R.id.user_row: {
            menu.add(PasswdSafe.CONTEXT_GROUP_RECORD_BASIC,
                     R.id.menu_copy_user, 0, R.string.copy_user);
            break;
        }
        case R.id.password_row: {
            menu.add(PasswdSafe.CONTEXT_GROUP_RECORD_BASIC,
                     R.id.menu_copy_password, 0, R.string.copy_password);
            break;
        }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item)
    {
        if (item.getGroupId() != PasswdSafe.CONTEXT_GROUP_RECORD_BASIC) {
            return super.onContextItemSelected(item);
        }

        switch (item.getItemId()) {
        case R.id.menu_copy_password: {
            copyPassword();
            return true;
        }
        case R.id.menu_copy_user: {
            copyUser();
            return true;
        }
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onClick(View view)
    {
        switch (view.getId()) {
        case R.id.base_row:
        case R.id.base_btn: {
            showRefRec(true, 0);
            break;
        }
        case R.id.password_row: {
            updatePasswordShown(true, 0);
            break;
        }
        }
    }

    @Override
    protected void doOnCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        inflater.inflate(R.menu.fragment_passwdsafe_record_basic, menu);
    }

    @Override
    protected void doRefresh(@NonNull RecordInfo info)
    {
        PwsRecord ref = info.itsPasswdRec.getRef();
        PwsRecord recForPassword = info.itsRec;
        int hiddenId = R.string.hidden_password_normal;
        String url = null;
        String email = null;
        String otp = null;
        Date creationTime = null;
        Date lastModTime = null;
        switch (info.itsPasswdRec.getType()) {
        case NORMAL: {
            itsBaseRow.setVisibility(View.GONE);
            url = info.itsFileData.getURL(info.itsRec);
            email = info.itsFileData.getEmail(info.itsRec);
            otp = info.itsFileData.getOtp(info.itsRec);
            creationTime = info.itsFileData.getCreationTime(info.itsRec);
            lastModTime = info.itsFileData.getLastModTime(info.itsRec);
            break;
        }
        case ALIAS: {
            itsBaseRow.setVisibility(View.VISIBLE);
            itsBaseLabel.setText(R.string.alias_base_record_header);
            itsBase.setText(info.itsFileData.getId(ref));
            hiddenId = R.string.hidden_password_alias;
            recForPassword = ref;
            url = info.itsFileData.getURL(info.itsRec);
            email = info.itsFileData.getEmail(info.itsRec);
            otp = info.itsFileData.getOtp(info.itsRec);
            creationTime = info.itsFileData.getCreationTime(recForPassword);
            lastModTime = info.itsFileData.getLastModTime(recForPassword);
            break;
        }
        case SHORTCUT: {
            itsBaseRow.setVisibility(View.VISIBLE);
            itsBaseLabel.setText(R.string.shortcut_base_record_header);
            itsBase.setText(info.itsFileData.getId(ref));
            hiddenId = R.string.hidden_password_shortcut;
            recForPassword = ref;
            creationTime = info.itsFileData.getCreationTime(recForPassword);
            lastModTime = info.itsFileData.getLastModTime(recForPassword);
            break;
        }
        }

        itsTitle = info.itsFileData.getTitle(info.itsRec);
        setFieldText(itsGroup, itsGroupRow,
                     info.itsFileData.getGroup(info.itsRec));
        setFieldText(itsUser, itsUserRow,
                     info.itsFileData.getUsername(info.itsRec));

        itsIsPasswordShown = false;
        itsHiddenPasswordStr = getString(hiddenId);
        String password = info.itsFileData.getPassword(recForPassword);
        setFieldText(itsPassword, itsPasswordRow,
                     ((password != null) ? itsHiddenPasswordStr : null));
        itsPasswordSeek.setMax((password != null) ? password.length() : 0);
        itsPasswordSeek.setProgress(0);

        setFieldText(itsUrl, itsUrlRow, url);
        setFieldText(itsEmail, itsEmailRow, email);

        if (otp != null && !otp.equals("")) {
            itsOtpTokenRow.setVisibility(View.VISIBLE);
        }

        GuiUtils.setVisible(itsTimesRow,
                            (creationTime != null) || (lastModTime != null));
        setFieldDate(itsCreationTime, itsCreationTimeRow, creationTime);
        setFieldDate(itsLastModTime, itsLastModTimeRow, lastModTime);
        GuiUtils.setVisible(itsProtectedRow,
                            info.itsFileData.isProtected(info.itsRec));

        List<PwsRecord> references = info.itsPasswdRec.getRefsToRecord();
        boolean hasReferences = (references != null) && !references.isEmpty();
        if (hasReferences) {
            ArrayAdapter<String> adapter =
                    new ArrayAdapter<>(getActivity(),
                                       android.R.layout.simple_list_item_1);
            for (PwsRecord refRec: references) {
                adapter.add(info.itsFileData.getId(refRec));
            }
            itsReferences.setAdapter(adapter);
        } else {
            itsReferences.setAdapter(null);
        }
        GuiUtils.setListViewHeightBasedOnChildren(itsReferences);
        GuiUtils.setVisible(itsReferencesRow, hasReferences);

        GuiUtils.invalidateOptionsMenu(getActivity());
    }

    private void generateOtpToken() {
        String otp = getOtp();

        Token token = null;
        try {
            PasswdSafeUtil.dbginfo("OTP", String.format("LOAD OTP: %s", otp));
            token = new Token(otp,false);
            itsOtp = token.generateCodes();

            if (token.getType() == Token.TokenType.HOTP) {
                PasswdSafeUtil.dbginfo("OTP", String.format("Saving incremented HOTP counter: %d", token.getCounter()));
                saveOtpChange(token.toString(), true);
            }
            setFieldText(itsOtpCode, null, itsOtp.getCurrentCode());

            itsOtpTimer.setProgress(itsOtp.getCurrentProgress());
        } catch (Exception e) {
            e.printStackTrace();
        }

        CountDownTimer otpTimeCountDown;
        otpTimeCountDown = new CountDownTimer(30000,500) {
            @Override
            public void onTick(long millisUntilFinished) {
                String currentCode = itsOtp.getCurrentCode();
                int currentProgress = itsOtp.getCurrentProgress();

                if (currentCode == null) {
                    setFieldText(itsOtpCode, null, "------");
                    itsOtpTimer.setProgress(0);
                    cancel();

                    return;
                }

                itsOtpTimer.setProgress( currentProgress );
                setFieldText(itsOtpCode, null, currentCode );
            }

            @Override
            public void onFinish() {
                start();
            }
        };
        otpTimeCountDown.start();
    }

    /**
     * Show a referenced record
     */
    private void showRefRec(final boolean baseRef, final int referencingPos)
    {
        final ObjectHolder<PasswdLocation> location = new ObjectHolder<>();
        useRecordInfo(new RecordInfoUser()
        {
            @Override
            public void useRecordInfo(@NonNull RecordInfo info)
            {
                PwsRecord refRec = null;
                if (baseRef) {
                    refRec = info.itsPasswdRec.getRef();
                } else {
                    List<PwsRecord> refs = info.itsPasswdRec.getRefsToRecord();
                    if ((referencingPos >= 0) &&
                        (referencingPos < refs.size())) {
                        refRec = refs.get(referencingPos);
                    }
                }
                if (refRec == null) {
                    return;
                }

                location.set(new PasswdLocation(refRec, info.itsFileData));
            }
        });
        if (location.get() != null) {
            getListener().changeLocation(location.get());
        }
    }

    /**
     * Update whether the password is shown
     */
    private void updatePasswordShown(boolean isToggle, int progress)
    {
        String password;
        if (isToggle) {
            itsIsPasswordShown = !itsIsPasswordShown;
            password = itsIsPasswordShown ? getPassword() : itsHiddenPasswordStr;
            itsPasswordSeek.setProgress(
                    itsIsPasswordShown ? itsPasswordSeek.getMax() : 0);
        } else if (progress == 0) {
            itsIsPasswordShown = false;
            password = itsHiddenPasswordStr;
        } else {
            itsIsPasswordShown = true;
            password = getPassword();
            if ((password != null) && (progress < password.length())) {
                password = password.substring(0, progress) + "…";
            }
        }
        itsPassword.setText(password);
        Activity act = getActivity();
        TypefaceUtils.enableMonospace(itsPassword, itsIsPasswordShown, act);
        GuiUtils.invalidateOptionsMenu(act);
    }

    /**
     * send credential over USB HID
     */
    private void sendCredentialUsb(OutputInterface.Language lang,
                                   Boolean sendUsername, Boolean sendPassword)
    {
        String username = getUsername();
        String password = getPassword();
        String quoteSubReturn = Pattern.quote(SUB_RETURN);
        String quoteSubTab = Pattern.quote(SUB_TAB);

        try {
            OutputInterface ct = new OutputKeyboardAsRoot(lang);

            boolean otpTokenGenerated = false;
            if( sendUsername == true && username != null ) {
                if (username.contains(SUB_OTP)){
                    generateOtpToken();
                    otpTokenGenerated = true;

                    username = username.replace(SUB_OTP, itsOtp.getCurrentCode());
                }

                String[] usernameArray = username.split(String.format("((?<=(%1$s|%2$s))|(?=(%1$s|%2$s)))", quoteSubReturn, quoteSubTab));
                PasswdSafeUtil.dbginfo(TAG, "Username Substitution Array: %s".format(Arrays.toString(usernameArray)));

                int ret = 0;
                for (String str : usernameArray){

                    if (str.equals(SUB_RETURN)) {
                        ct.sendReturn();
                    } else if (str.equals(SUB_TAB)) {
                        ct.sendTabulator();
                    } else {
                        ret = ct.sendText(str);
                    }

                    if (ret == 1) {
                        PasswdSafeUtil.showErrorMsg(
                                "Lost characters in output due to missing mapping!",
                                getContext());
                    }
                }
            }

            if( sendUsername && sendPassword )
            {
                int checkedId = itsSendDelimiter.getCheckedRadioButtonId();
                if (checkedId == R.id.send_delimiter_return) {
                    ct.sendReturn();
                } else if (checkedId == R.id.send_delimiter_tab) {
                    ct.sendTabulator();
                }
            }

            if( sendPassword && password != null ) {
                if (password.contains(SUB_OTP)){
                    if (otpTokenGenerated == false) {
                        generateOtpToken();
                    }

                    password = password.replace(SUB_OTP, itsOtp.getCurrentCode());
                }

                String[] passwordArray = password.split(String.format("((?<=(%1$s|%2$s))|(?=(%1$s|%2$s)))", quoteSubReturn, quoteSubTab));
                PasswdSafeUtil.dbginfo(TAG, "Password Substitution Array: %s".format(Arrays.toString(passwordArray)));

                int ret = 0;
                for (String str : passwordArray){

                    if (str.equals(SUB_RETURN)) {
                        ct.sendReturn();
                    } else if (str.equals(SUB_TAB)) {
                        ct.sendTabulator();
                    } else {
                        ret = ct.sendText(str);
                    }

                    if (ret == 1) {
                        PasswdSafeUtil.showErrorMsg(
                                "Lost characters in output due to missing mapping!",
                                getContext());
                    }
                }

                if( itsSendReturnSuffix.isChecked() ) {
                    ct.sendReturn();
                }
            }

            ct.destruct();

        } catch (Exception e) {
            PasswdSafeUtil.dbginfo("PasswdSafeRecordBasicFragment", e, e.getLocalizedMessage());
        }

    }

    /**
     * send credential over USB HID
     */
    private void sendCredentialUsb(String usbKbdLang, Boolean sendUsername, Boolean sendPassword)
    {

        if( usbKbdLang.equals("null") ) {
            PasswdSafeUtil.dbginfo("PasswdSafeRecordBasicFragment", "Getting systems language default");
            usbKbdLang = Locale.getDefault().toString();
        }

        PasswdSafeUtil.dbginfo("PasswdSafeRecordBasicFragment", "Identified language: " + usbKbdLang);

        OutputInterface.Language lang;
        try {
            lang = OutputInterface.Language.valueOf(usbKbdLang);
        } catch (IllegalArgumentException e) {
            PasswdSafeUtil.dbginfo("PasswdSafeRecordBasicFragment", "No scancode mapping for '" + usbKbdLang +"' - using en_US!");
            lang = OutputInterface.Language.en_US;
        }

        sendCredentialUsb(lang, sendUsername, sendPassword);
    }

    public void sendCredentialUsbCustomLang(final Boolean sendUsername,
                                            final Boolean sendPassword)
    {

        // Build an AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle(R.string.title_usbkbd_language)
               .setItems(R.array.usbkbd_languages_titels, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int which) {
                       sendCredentialUsb( getResources().getStringArray(R.array.usbkbd_languages_values)[which], sendUsername, sendPassword );
                   }
               });

        AlertDialog dialog = builder.create();
        // Display the alert dialog on interface
        dialog.show();
    }

    private void startOtpAddActivity(final boolean manual) {
        if (hasOtp()) {
            Context ctx = getContext();
            LayoutInflater factory = LayoutInflater.from(ctx);
            View dlgView = factory.inflate(R.layout.confirm_prompt, null);

            final CheckBox itsConfirmCb = (CheckBox)dlgView.findViewById(R.id.confirm);
            AlertDialog.Builder alert = new AlertDialog.Builder(ctx)
                    .setTitle(getString(R.string.otp_overwrite))
                    .setView(dlgView)
                    .setPositiveButton(R.string.replace, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int whichButton) {
                            if (itsConfirmCb.isChecked()) {
                                if(manual) {
                                    startOtpManualAddActivity();
                                } else {
                                    checkAndStartOtpCameraAddActivity();
                                }
                            }
                        }
                    })
                    .setNegativeButton(R.string.cancel, null);
            alert.show();
        } else if (manual) {
            startOtpManualAddActivity();
        } else {
            checkAndStartOtpCameraAddActivity();
        }
    }

    private void startOtpManualAddActivity() {
        Intent intent = new Intent(getActivity(), AddActivity.class);
        startActivityForResult(intent, 1);
    }

    private void startOtpCameraAddActivity() {
        Intent intent = new Intent(getActivity(), ScanActivity.class);
        startActivityForResult(intent, 2);

        getActivity().overridePendingTransition(R.xml.fadein, R.xml.fadeout);
    }

    private void checkAndStartOtpCameraAddActivity() {
        if (ActivityCompat.checkSelfPermission(getContext(), android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, PERMISSIONS_REQUEST_CAMERA);
        }
        else {
            // permission is already granted
            startOtpCameraAddActivity();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
        case PERMISSIONS_REQUEST_CAMERA: {
            if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startOtpCameraAddActivity();
            } //else {
            //Toast.makeText(MainActivity.this, R.string.error_permission_camera_open, Toast.LENGTH_LONG).show();
            //}
            return;
        }
        }
    }

    /**
     * Copy the user name to the clipboard
     */
    private void copyUser()
    {
        getListener().copyField(CopyField.USER_NAME, getLocation().getRecord());
    }

    /**
     * Copy the password to the clipboard
     */
    private void copyPassword()
    {
        getListener().copyField(CopyField.PASSWORD, getLocation().getRecord());
    }

    /**
     * Copy the URL to the clipboard
     */
    private void copyUrl()
    {
        getListener().copyField(CopyField.URL, getLocation().getRecord());
    }

    /**
     * Copy the email to the clipboard
     */
    private void copyEmail()
    {
        getListener().copyField(CopyField.EMAIL, getLocation().getRecord());
    }

    /**
     * Get the username
     */
    private String getUsername()
    {
        final ObjectHolder<String> username = new ObjectHolder<>();
        useRecordInfo(new RecordInfoUser()
        {
            @Override
            public void useRecordInfo(@NonNull RecordInfo info)
            {
                username.set(info.itsFileData.getUsername(info.itsRec));
            }
        });
        return username.get();
    }

    /**
     * Get the password
     */
    private String getPassword()
    {
        final ObjectHolder<String> password = new ObjectHolder<>();
        useRecordInfo(new RecordInfoUser()
        {
            @Override
            public void useRecordInfo(@NonNull RecordInfo info)
            {
                password.set(info.itsPasswdRec.getPassword(info.itsFileData));
            }
        });
        return password.get();
    }

    /**
     * Get the otp
     */
    private String getOtp()
    {
        final ObjectHolder<String> otp = new ObjectHolder<>();
        useRecordInfo(new RecordInfoUser()
        {
            @Override
            public void useRecordInfo(@NonNull RecordInfo info)
            {
                otp.set(info.itsFileData.getOtp(info.itsRec));
            }
        });
        return otp.get();
    }

    /**
     * Check if otp is set
     */
    private boolean hasOtp()
    {
        final ObjectHolder<Boolean> otp = new ObjectHolder<>();
        useRecordInfo(new RecordInfoUser()
        {
            @Override
            public void useRecordInfo(@NonNull RecordInfo info)
            {
                otp.set(false);
                String otpUri = info.itsFileData.getOtp(info.itsRec);
                if (otpUri != null && !otpUri.equals("")) {
                    otp.set(true);
                }
            }
        });
        return otp.get();
    }
}
