/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.blink.browser;

public interface PreferenceKeys {

    static final String PREF_AUTOFILL_ACTIVE_PROFILE_ID = "autofill_active_profile_id";
    static final String PREF_DEBUG_MENU = "debug_menu";

    // ----------------------
    // Keys for accessibility_preferences.xml
    // ----------------------
    static final String PREF_MIN_FONT_SIZE = "min_font_size";
    static final String PREF_TEXT_SIZE = "text_size";
    static final String PREF_TEXT_ZOOM = "text_zoom";
    static final String PREF_DOUBLE_TAP_ZOOM = "double_tap_zoom";
    static final String PREF_FORCE_USERSCALABLE = "force_userscalable";

    // ----------------------
    // Keys for settings_preferences.xml
    // ----------------------
    static final String PREF_WEB_BROWSING = "web_browsing";
    static final String PREF_PRIVACY_SECURITY = "privacy_security";
    static final String PREF_ADVANCED = "advanced";
    static final String PREF_APP_NAME = "app_name";
    static final String PREF_CUSTOM_DOWNLOAD_PATH = "custom_download_path";
    static final String PREF_NIGHT_MODE = "night_mode";
    static final String PREF_FULLSCREEN = "fullscreen";
    static final String PREF_TEMP_FULLSCREEN = "temp_fullscreen";
    static final String PREF_DEFAULT_BROWSER = "default_browser";
    static final String PREF_APP_SCORE = "app_score";
    static final String PREF_SHOW_STATUS_BAR = "show_status_bar";
    static final String PREF_DOWNLOAD = "download";
    static final String PREF_LOCK_TOOLBAR = "lock_toolbar";
    static final String PREF_ADBLOCK_UPDATE = "adblock_update";
    static final String PREF_EASYLIST_UPDATE = "easylist_update";

    // ----------------------
    // Keys for advanced_preferences.xml
    // ----------------------
    static final String PREF_AUTOFIT_PAGES = "autofit_pages";
    static final String PREF_BLOCK_POPUP_WINDOWS = "block_popup_windows";
    static final String PREF_DEFAULT_TEXT_ENCODING = "default_text_encoding";
    static final String PREF_ENABLE_JAVASCRIPT = "enable_javascript";
    static final String PREF_ENABLE_COOKIES_INCOGNITO = "enable_cookies_incognito";
    static final String PREF_RESET_DEFAULT_PREFERENCES = "reset_default_preferences";
    static final String PREF_SEARCH_ENGINE = "search_engine";
    static final String PREF_WEBSITE_SETTINGS = "website_settings";
    static final String PREF_SEARCH_SUGGESTIONS = "search_suggestions";
    static final String PREF_RESTORE_TABS_ON_STARTUP = "restore_tabs_on_startup";
    static final String PREF_HOMEPAGE_PICKER = "homepage_picker";
    static final String PREF_NOTIFICATION_TOOL_SHOW = "notification_tool_show";

    static final String SEARCH_ENGINE_LOAD_TIME = "search_engine_load_time";

    // ----------------------
    // Keys for debug_preferences.xml
    // ----------------------
    static final String PREF_ENABLE_HARDWARE_ACCEL = "enable_hardware_accel";
    static final String PREF_ENABLE_HARDWARE_ACCEL_SKIA = "enable_hardware_accel_skia";
    static final String PREF_USER_AGENT = "user_agent";
    static final String PREF_DISABLE_PERF = "disable_perf";

    // ----------------------
    // Keys for general_preferences.xml
    // ----------------------
    static final String PREF_AUTOFILL_ENABLED = "autofill_enabled";
    static final String PREF_AUTOFILL_PROFILE = "autofill_profile";
    static final String PREF_HOMEPAGE = "homepage";
    static final String PREF_HOMEPAGE_CHANGED = "homepage_changed";
    static final String PREF_SYNC_WITH_CHROME = "sync_with_chrome";

    // ----------------------
    // Keys for hidden_debug_preferences.xml
    // ----------------------
    static final String PREF_ENABLE_LIGHT_TOUCH = "enable_light_touch";
    static final String PREF_ENABLE_NAV_DUMP = "enable_nav_dump";
    static final String PREF_ENABLE_TRACING = "enable_tracing";
    static final String PREF_ENABLE_VISUAL_INDICATOR = "enable_visual_indicator";
    static final String PREF_ENABLE_CPU_UPLOAD_PATH = "enable_cpu_upload_path";
    static final String PREF_JAVASCRIPT_CONSOLE = "javascript_console";
    static final String PREF_JS_ENGINE_FLAGS = "js_engine_flags";
    static final String PREF_NORMAL_LAYOUT = "normal_layout";
    static final String PREF_WIDE_VIEWPORT = "wide_viewport";
    static final String PREF_DEVERLOPER_OPTIONS = "deverloper_options";
    static final String PREF_OPEN_DEBUG = "open_debug";
    static final String PREF_CHANNEL = "channel";
    static final String PREF_NEW_FEATURES = "new_features";

    // ----------------------
    // Keys for lab_preferences.xml
    // ----------------------
    static final String PREF_ENABLE_QUICK_CONTROLS = "enable_quick_controls";

    // ----------------------
    // Keys for privacy_security_preferences.xml
    // ----------------------
    static final String PREF_CLEAR_DATA = "privacy_clear_data";
    static final String PREF_CLEAR_SELECTED_DATA = "privacy_clear_selected";
    static final String PREF_ACCEPT_COOKIES = "accept_cookies";
    static final String PREF_ENABLE_GEOLOCATION = "enable_geolocation";
    static final String PREF_PRIVACY_CLEAR_CACHE = "privacy_clear_cache";
    static final String PREF_PRIVACY_CLEAR_COOKIES = "privacy_clear_cookies";
    static final String PREF_PRIVACY_CLEAR_FORM_DATA = "privacy_clear_form_data";
    static final String PREF_PRIVACY_CLEAR_GEOLOCATION_ACCESS = "privacy_clear_geolocation_access";
    static final String PREF_PRIVACY_CLEAR_HISTORY = "privacy_clear_history";
    static final String PREF_PRIVACY_CLEAR_PASSWORDS = "privacy_clear_passwords";
    static final String PREF_REMEMBER_PASSWORDS = "remember_passwords";
    static final String PREF_SAVE_FORMDATA = "save_formdata";
    static final String PREF_SHOW_SECURITY_WARNINGS = "show_security_warnings";
    static final String PREF_CLEAR_HISTORY_CACHE_EXITING = "clear_history_cache_exiting";
    static final String PREF_AD_BLOCK = "ad_block";
    static final String PREF_IMG_AD_BLOCK_COUNT = "img_ad_block_count";
    static final String PREF_JS_AD_BLOCK_COUNT = "js_ad_block_count";
    static final String PREF_POPUP_AD_BLOCK_COUNT = "popup_ad_block_count";
    static final String PREF_CONFIRM_ON_EXIT = "confirm_on_exit";

    // ----------------------
    // Keys for About_preferences.xml
    // ----
    static final String PREF_CONTACT_US = "contact_us";
    static final String PREF_FEEDBACK = "feedback";
    static final String PREF_VERSION = "version";
    static final String PREF_CHECK_UPDATE = "check_update";
    static final String PREF_TERMS_PRIVACY = "terms_privacy";

    static final String PREF_DOWNLOAD_DIALOG_SHOW = "download_dialog_show";
    static final String PREF_DOWNLOAD_ADM = "download_adm";

    /**
     * 是否请求过引擎
     */
    public static final String IS_FIRST_CREATE_ENGINES = "is_first_create_engines";

    // ----------------------
    // Keys for bandwidth_preferences.xml
    // ----------------------
    static final String PREF_LOAD_IMAGES = "load_images";

    // ----------------------
    // Keys for browser recovery
    // ----------------------
    /**
     * The last time recovery was started as System.currentTimeMillis.
     * 0 if not set.
     */
    static final String KEY_LAST_RECOVERED = "last_recovered";

    /**
     * Key for whether or not the last run was paused.
     */
    static final String KEY_LAST_RUN_PAUSED = "last_paused";

    static final String DEFAULT_CACHE_DATA_COPIED = "pref_default_cache_data_copied";

    static final String DEFAULT_CACHE_WEB_ICONS = "pref_default_cache_web_icons";

    static final String RESTRICTIONS = "App Restrictions";

    static final String LAST_READ_ALLOW_GEOLOCATION_ORIGINS = "last_read_allow_geolocation_origins";

    static final String BRIGHTNESS = "brightness";
}
