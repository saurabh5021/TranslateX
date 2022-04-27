package com.example.translatex;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;

import android.os.Bundle;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.provider.MediaStore;
import android.speech.RecognizerIntent;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class TranslateFragment extends Fragment {
    public static TranslateFragment newInstance() {
        return new TranslateFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);
    }

    ActivityResultLauncher<String[]> permissionLauncher;
    private boolean isRecordPermissionGranted = false;
    private boolean isCameraPermissionGranted = false;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.translate_fragment, container, false);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
         Button switchButton = view.findViewById(R.id.buttonSwitchLang);
         ToggleButton sourceSyncButton = view.findViewById(R.id.buttonSyncSource);
         ToggleButton targetSyncButton = view.findViewById(R.id.buttonSyncTarget);
         TextInputEditText srcTextView = view.findViewById(R.id.sourceText);
         TextView targetTextView = view.findViewById(R.id.targetText);
         TextView downloadedModelsTextView = view.findViewById(R.id.downloadedModels);
         Spinner sourceLangSelector = view.findViewById(R.id.sourceLangSelector);
         Spinner targetLangSelector = view.findViewById(R.id.targetLangSelector);
         ImageView micIV = view.findViewById(R.id.mic);
        ImageView cameraIV = view.findViewById(R.id.camera);

        TranslateViewModel viewModel = new ViewModelProvider(this).get(TranslateViewModel.class);

        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), new ActivityResultCallback<Map<String, Boolean>>() {
            @Override
            public void onActivityResult(Map<String, Boolean> result) {
                if(result.get(Manifest.permission.RECORD_AUDIO) != null){
                    isRecordPermissionGranted = result.get(Manifest.permission.RECORD_AUDIO);
                }

                if(result.get(Manifest.permission.CAMERA) != null){
                    isRecordPermissionGranted = result.get(Manifest.permission.CAMERA);
                }
            }
        });

        checkPermission();

        final ArrayAdapter<TranslateViewModel.Language> adapter =
                new ArrayAdapter<>(getContext(),
                        android.R.layout.simple_spinner_dropdown_item,
                        viewModel.getAvailableLanguages());
        sourceLangSelector.setAdapter(adapter);
        targetLangSelector.setAdapter(adapter);
        sourceLangSelector.setSelection(adapter.getPosition(new TranslateViewModel.Language("en")));
        targetLangSelector.setSelection(adapter.getPosition(new TranslateViewModel.Language("hi")));
        sourceLangSelector.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        setProgressText(targetTextView);
                        viewModel.sourceLang.setValue(adapter.getItem(position));
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        targetTextView.setText("");
                    }
                });
        targetLangSelector.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        setProgressText(targetTextView);
                        viewModel.targetLang.setValue(adapter.getItem(position));
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        targetTextView.setText("");
                    }
                });

        switchButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String targetText = targetTextView.getText().toString();
                        setProgressText(targetTextView);
                        int sourceLangPosition = sourceLangSelector.getSelectedItemPosition();
                        sourceLangSelector.setSelection(targetLangSelector.getSelectedItemPosition());
                        targetLangSelector.setSelection(sourceLangPosition);

                        // Also update srcTextView with targetText
                        srcTextView.setText(targetText);
                        viewModel.sourceText.setValue(targetText);
                    }
                });

        // Set up toggle buttons to delete or download remote models locally.
        sourceSyncButton.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        TranslateViewModel.Language language =
                                adapter.getItem(sourceLangSelector.getSelectedItemPosition());
                        if (isChecked) {
                            viewModel.downloadLanguage(language);
                        } else {
                            viewModel.deleteLanguage(language);
                        }
                    }
                });


        ActivityResultLauncher<Intent> launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                if(result.getResultCode() == getActivity().RESULT_OK && result.getData() != null){
                    Intent data = result.getData();
                    ArrayList<String> result2 = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                            srcTextView.setText(null);
                            srcTextView.setText(result2.get(0));
                }

            }
        });

        micIV.setOnClickListener(view1 -> {
            Intent  intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak To Convert to Text");

            launcher.launch(intent);

        });


        ActivityResultLauncher<Intent> launcherCamera = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
                Bundle bundle = result.getData().getExtras();
                bundle.get("data");
                Bitmap bitmap = (Bitmap) bundle.get("data");
                InputImage image = InputImage.fromBitmap(bitmap, Surface.ROTATION_0);
                Task<Text> task = recognizer.process(image);
                task.addOnSuccessListener(new OnSuccessListener<Text>() {
                    @Override
                    public void onSuccess(Text VisionText) {
                        srcTextView.setText(null);
                        String result = VisionText.getText();
                        srcTextView.setText(result);

                    }
                });
                task.addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getActivity(), ""+e.getMessage(), Toast.LENGTH_SHORT).show();

                    }
                });




            }
        });

        cameraIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                launcherCamera.launch(intent);

            }
        });

        targetSyncButton.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        TranslateViewModel.Language language =
                                adapter.getItem(targetLangSelector.getSelectedItemPosition());
                        if (isChecked) {
                            viewModel.downloadLanguage(language);
                        } else {
                            viewModel.deleteLanguage(language);
                        }
                    }
                });

        // Translate input text as it is typed
        srcTextView.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}

                    @Override
                    public void afterTextChanged(Editable s) {
                        setProgressText(targetTextView);
                        viewModel.sourceText.postValue(s.toString());
                    }
                });
        viewModel.translatedText.observe(
                getViewLifecycleOwner(),
                new Observer<TranslateViewModel.ResultOrError>() {
                    @Override
                    public void onChanged(TranslateViewModel.ResultOrError resultOrError) {
                        if (resultOrError.error != null) {
                            srcTextView.setError(resultOrError.error.getLocalizedMessage());
                        } else {
                            targetTextView.setText(resultOrError.result);
                        }
                    }
                });

        // Update sync toggle button states based on downloaded models list.
        viewModel.availableModels.observe(
                getViewLifecycleOwner(),
                new Observer<List<String>>() {
                    @Override
                    public void onChanged(@Nullable List<String> translateRemoteModels) {
                        String output =
                                getContext().getString(R.string.downloaded_models_label, translateRemoteModels);
                        downloadedModelsTextView.setText(output);
                        sourceSyncButton.setChecked(
                                translateRemoteModels.contains(
                                        adapter.getItem(sourceLangSelector.getSelectedItemPosition()).getCode()));
                        targetSyncButton.setChecked(
                                translateRemoteModels.contains(
                                        adapter.getItem(targetLangSelector.getSelectedItemPosition()).getCode()));
                    }
                });
    }

    private void checkPermission() {

        isRecordPermissionGranted = ContextCompat.checkSelfPermission(getActivity(),Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
        isCameraPermissionGranted = ContextCompat.checkSelfPermission(getActivity(),Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;

        List<String> permissionRequest = new ArrayList<String>();

        if(!isRecordPermissionGranted){
            permissionRequest.add(Manifest.permission.RECORD_AUDIO);
        }

        if(!isCameraPermissionGranted){
            permissionRequest.add(Manifest.permission.CAMERA);
        }

        if(!permissionRequest.isEmpty()){
            permissionLauncher.launch(permissionRequest.toArray(new String[0]));
        }


    }


    private void setProgressText(TextView tv) {
        tv.setText(getContext().getString(R.string.translate_progress));
    }


}