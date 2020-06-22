package com.connectycube.sample.conference.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.connectycube.sample.conference.R;
import com.connectycube.videochat.RTCMediaConfig;

import java.util.List;


public class SettingsUtil {

    private static final String TAG = SettingsUtil.class.getSimpleName();

    private static void setSettingsForMultiCall(List<Integer> users) {
        if (users.size() <= 4) {
            setDefaultVideoQuality();
        } else {
            //set to minimum settings
            RTCMediaConfig.setVideoWidth(RTCMediaConfig.VideoQuality.QVGA_VIDEO.width);
            RTCMediaConfig.setVideoHeight(RTCMediaConfig.VideoQuality.QVGA_VIDEO.height);
            RTCMediaConfig.setVideoHWAcceleration(false);
            RTCMediaConfig.setVideoCodec(null);
        }
    }

    public static void setSettingsStrategy(List<Integer> users, SharedPreferences sharedPref, Context context) {
        setCommonSettings(sharedPref, context);
        if (users.size() == 1) {
            setSettingsFromPreferences(sharedPref, context);
        } else {
            setSettingsForMultiCall(users);
        }
    }

    private static void setCommonSettings(SharedPreferences sharedPref, Context context) {
        String audioCodecDescription = getPreferenceString(sharedPref, context, R.string.pref_audiocodec_key,
                R.string.pref_audiocodec_def);
        RTCMediaConfig.AudioCodec audioCodec = RTCMediaConfig.AudioCodec.ISAC.getDescription()
                .equals(audioCodecDescription) ?
                RTCMediaConfig.AudioCodec.ISAC : RTCMediaConfig.AudioCodec.OPUS;
        Log.e(TAG, "audioCodec =: " + audioCodec.getDescription());
        RTCMediaConfig.setAudioCodec(audioCodec);
        Log.v(TAG, "audioCodec = " + RTCMediaConfig.getAudioCodec());
        // Check Disable built-in AEC flag.
        boolean disableBuiltInAEC = getPreferenceBoolean(sharedPref, context,
                R.string.pref_disable_built_in_aec_key,
                R.string.pref_disable_built_in_aec_default);

        RTCMediaConfig.setUseBuildInAEC(!disableBuiltInAEC);
        Log.v(TAG, "setUseBuildInAEC = " + RTCMediaConfig.isUseBuildInAEC());
        // Check Disable Audio Processing flag.
        boolean noAudioProcessing = getPreferenceBoolean(sharedPref, context,
                R.string.pref_noaudioprocessing_key,
                R.string.pref_noaudioprocessing_default);
        RTCMediaConfig.setAudioProcessingEnabled(!noAudioProcessing);
        Log.v(TAG, "isAudioProcessingEnabled = " + RTCMediaConfig.isAudioProcessingEnabled());
        // Check OpenSL ES enabled flag.
        boolean useOpenSLES = getPreferenceBoolean(sharedPref, context,
                R.string.pref_opensles_key,
                R.string.pref_opensles_default);
        RTCMediaConfig.setUseOpenSLES(useOpenSLES);
        Log.v(TAG, "isUseOpenSLES = " + RTCMediaConfig.isUseOpenSLES());
    }

    private static void setSettingsFromPreferences(SharedPreferences sharedPref, Context context) {

        // Check HW codec flag.
        boolean hwCodec = sharedPref.getBoolean(context.getString(R.string.pref_hwcodec_key),
                Boolean.valueOf(context.getString(R.string.pref_hwcodec_default)));

        RTCMediaConfig.setVideoHWAcceleration(hwCodec);

        // Get video resolution from settings.
        int resolutionItem = Integer.parseInt(sharedPref.getString(context.getString(R.string.pref_resolution_key),
                "0"));
        Log.e(TAG, "resolutionItem =: " + resolutionItem);
        setVideoQuality(resolutionItem);
        Log.v(TAG, "resolution = " + RTCMediaConfig.getVideoHeight() + "x" + RTCMediaConfig.getVideoWidth());

        // Get start bitrate.
        int startBitrate = getPreferenceInt(sharedPref, context,
                R.string.pref_startbitratevalue_key,
                R.string.pref_startbitratevalue_default);
        Log.e(TAG, "videoStartBitrate =: " + startBitrate);
        RTCMediaConfig.setVideoStartBitrate(startBitrate);
        Log.v(TAG, "videoStartBitrate = " + RTCMediaConfig.getVideoStartBitrate());

        int videoCodecItem = Integer.parseInt(getPreferenceString(sharedPref, context, R.string.pref_videocodec_key, "0"));
        for (RTCMediaConfig.VideoCodec codec : RTCMediaConfig.VideoCodec.values()) {
            if (codec.ordinal() == videoCodecItem) {
                Log.e(TAG, "videoCodecItem =: " + codec.getDescription());
                RTCMediaConfig.setVideoCodec(codec);
                Log.v(TAG, "videoCodecItem = " + RTCMediaConfig.getVideoCodec());
                break;
            }
        }
        // Get camera fps from settings.
        int cameraFps = getPreferenceInt(sharedPref, context, R.string.pref_frame_rate_key, R.string.pref_frame_rate_default);
        Log.e(TAG, "cameraFps = " + cameraFps);
        RTCMediaConfig.setVideoFps(cameraFps);
        Log.v(TAG, "cameraFps = " + RTCMediaConfig.getVideoFps());
    }

    private static void setVideoQuality(int resolutionItem) {
        if (resolutionItem != -1) {
            setVideoFromLibraryPreferences(resolutionItem);
        } else {
            setDefaultVideoQuality();
        }
    }

    private static void setDefaultVideoQuality() {
        RTCMediaConfig.setVideoWidth(RTCMediaConfig.VideoQuality.VGA_VIDEO.width);
        RTCMediaConfig.setVideoHeight(RTCMediaConfig.VideoQuality.VGA_VIDEO.height);
    }

    private static void setVideoFromLibraryPreferences(int resolutionItem) {
        for (RTCMediaConfig.VideoQuality quality : RTCMediaConfig.VideoQuality.values()) {
            if (quality.ordinal() == resolutionItem) {
                Log.e(TAG, "resolution =: " + quality.height + ":" + quality.width);
                RTCMediaConfig.setVideoHeight(quality.height);
                RTCMediaConfig.setVideoWidth(quality.width);
            }
        }
    }

    private static String getPreferenceString(SharedPreferences sharedPref, Context context, int strResKey, int strResDefValue) {
        return sharedPref.getString(context.getString(strResKey), context.getString(strResDefValue));
    }

    private static String getPreferenceString(SharedPreferences sharedPref, Context context, int strResKey, String strResDefValue) {
        return sharedPref.getString(context.getString(strResKey), strResDefValue);
    }

    public static int getPreferenceInt(SharedPreferences sharedPref, Context context, int strResKey, int strResDefValue) {
        return sharedPref.getInt(context.getString(strResKey), Integer.valueOf(context.getString(strResDefValue)));
    }

    private static boolean getPreferenceBoolean(SharedPreferences sharedPref, Context context, int StrRes, int strResDefValue) {
        return sharedPref.getBoolean(context.getString(StrRes), Boolean.valueOf(context.getString(strResDefValue)));
    }
}
