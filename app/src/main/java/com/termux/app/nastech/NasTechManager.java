package com.termux.app.nastech;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * NasTech AI Terminal System — Core Manager
 * Handles startup init, environment setup, and $ command injection.
 */
public class NasTechManager {

    private static final String PREFS = "nastech_prefs";
    private static final String KEY_INITIALIZED = "nastech_initialized";
    private static final String KEY_AI_MODEL = "nastech_ai_model";
    private static final String KEY_BIOMETRIC = "biometric_lock";
    private static final String KEY_API_KEY = "openrouter_api_key";

    private static Context sAppContext;

    public static void init(Context context) {
        sAppContext = context.getApplicationContext();
        SharedPreferences prefs = sAppContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        // Set sensible defaults on first run
        if (!prefs.getBoolean(KEY_INITIALIZED, false)) {
            prefs.edit()
                .putBoolean(KEY_INITIALIZED, true)
                .putString(KEY_AI_MODEL, "openai/gpt-4o")
                .putBoolean(KEY_BIOMETRIC, false)
                .apply();
        }

        // Ensure NasTech home directory exists on device
        File nasTechDir = new File(System.getenv("HOME") != null
            ? System.getenv("HOME") + "/.nastech"
            : context.getFilesDir() + "/.nastech");
        if (!nasTechDir.exists()) nasTechDir.mkdirs();

        // Write the NasTech shell init ($ command prefix + AI stub)
        writeNasTechInit(nasTechDir, prefs.getString(KEY_API_KEY, ""));
    }

    private static void writeNasTechInit(File dir, String apiKey) {
        String rcContent =
            "# NasTech AI Terminal System — Auto-generated\n" +
            "export NASTECH_HOME=\"" + dir.getAbsolutePath() + "\"\n" +
            "export NASTECH_MODEL=\"openai/gpt-4o\"\n" +
            "export NASTECH_VERSION=\"v6\"\n" +
            "export OPENROUTER_API_KEY=\"" + apiKey + "\"\n\n" +
            "# $ command prefix\n" +
            "nastech_cmd() {\n" +
            "  local cmd=\"$1\"; shift\n" +
            "  case \"$cmd\" in\n" +
            "    ai)      python3 \"$NASTECH_HOME/nastech_ai.py\" \"$@\" ;;\n" +
            "    ubuntu)  bash \"$NASTECH_HOME/ubuntu_layer.sh\" \"$@\" ;;\n" +
            "    install) bash \"$NASTECH_HOME/nastech_engine_v6.sh\" \"$@\" ;;\n" +
            "    system)  python3 \"$NASTECH_HOME/nastech_audit.py\" \"$@\" ;;\n" +
            "    git)     bash \"$NASTECH_HOME/nastech_git.sh\" \"$@\" ;;\n" +
            "    speak)   bash \"$NASTECH_HOME/nastech_speak.sh\" \"$@\" ;;\n" +
            "    help)    echo -e \"\\033[1;36mNasTech v6 Commands:\\033[0m\"\n" +
            "             echo \"  $ ai [prompt]    — Stream AI response\"\n" +
            "             echo \"  $ ubuntu         — Enter Ubuntu proot\"\n" +
            "             echo \"  $ install [pkg]  — NasTech v6 installer\"\n" +
            "             echo \"  $ system         — System audit\"\n" +
            "             echo \"  $ git [cmd]      — Git operations\"\n" +
            "             echo \"  $ speak [text]   — Piper TTS (offline voice)\"\n" +
            "             ;;\n" +
            "    *)       echo \"Unknown NasTech command: $cmd. Try: $ help\" ;;\n" +
            "  esac\n" +
            "}\n" +
            "alias \\$='nastech_cmd'\n\n" +
            "# Source NasTech on terminal open\n" +
            "echo -e \"\\033[1;36m⬡ NasTech AI Terminal v6 — ready. Type: $ help\\033[0m\"\n";

        // Write the Piper TTS speak script alongside the init script
        writeSpeakScript(dir);

        try {
            File rcFile = new File(dir, "nastech_init.sh");
            try (FileOutputStream fos = new FileOutputStream(rcFile)) {
                fos.write(rcContent.getBytes());
            }
            // Append source line to bashrc if not already there
            File bashrc = new File(System.getenv("HOME") != null
                ? System.getenv("HOME") + "/.bashrc"
                : sAppContext.getFilesDir() + "/.bashrc");
            String sourceLine = "\n# NasTech AI\n[ -f \"$NASTECH_HOME/nastech_init.sh\" ] && source \"$NASTECH_HOME/nastech_init.sh\"\n";
            if (bashrc.exists()) {
                String existing = new String(readFile(bashrc));
                if (!existing.contains("nastech_init.sh")) {
                    try (FileOutputStream fos = new FileOutputStream(bashrc, true)) {
                        fos.write(sourceLine.getBytes());
                    }
                }
            } else {
                try (FileOutputStream fos = new FileOutputStream(bashrc)) {
                    fos.write(sourceLine.getBytes());
                }
            }
        } catch (IOException ignored) {}
    }

    private static void writeSpeakScript(File dir) {
        String speakScript =
            "#!/data/data/com.termux/files/usr/bin/bash\n" +
            "# NasTech AI Terminal — Piper TTS offline voice engine\n" +
            "# Usage: $ speak [text...]\n\n" +
            "PIPER_DIR=\"$NASTECH_HOME/piper\"\n" +
            "VOICE_DIR=\"$NASTECH_HOME/voices\"\n" +
            "VOICE_MODEL=\"$VOICE_DIR/en_US-lessac-medium.onnx\"\n" +
            "VOICE_CONFIG=\"$VOICE_DIR/en_US-lessac-medium.onnx.json\"\n" +
            "VOICE_URL=\"https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/lessac/medium\"\n" +
            "TMP_WAV=\"/tmp/nastech_speak.wav\"\n\n" +
            "# Collect text\n" +
            "TEXT=\"$*\"\n" +
            "if [ -z \"$TEXT\" ]; then\n" +
            "  echo -e \"\\033[1;36mUsage: $ speak [text]\\033[0m\"\n" +
            "  echo \"  Example: $ speak Hello from NasTech AI\"\n" +
            "  exit 0\n" +
            "fi\n\n" +
            "# Step 1 — ensure piper binary\n" +
            "if ! command -v piper &>/dev/null && [ ! -f \"$PIPER_DIR/piper\" ]; then\n" +
            "  echo -e \"\\033[1;33m⬡ Installing Piper TTS…\\033[0m\"\n" +
            "  mkdir -p \"$PIPER_DIR\"\n" +
            "  # Try pip first (works in Termux with Python)\n" +
            "  if command -v pip3 &>/dev/null; then\n" +
            "    pip3 install --quiet piper-tts 2>/dev/null && PIPER_CMD=\"python3 -m piper\"\n" +
            "  fi\n" +
            "  # Fallback: download piper binary for aarch64\n" +
            "  if [ -z \"$PIPER_CMD\" ]; then\n" +
            "    ARCH=$(uname -m)\n" +
            "    PIPER_RELEASE=\"https://github.com/rhasspy/piper/releases/download/2023.11.14-2\"\n" +
            "    if [ \"$ARCH\" = \"aarch64\" ]; then\n" +
            "      curl -sL \"$PIPER_RELEASE/piper_linux_aarch64.tar.gz\" | tar -xz -C \"$PIPER_DIR\" --strip-components=1\n" +
            "    elif [ \"$ARCH\" = \"armv7l\" ]; then\n" +
            "      curl -sL \"$PIPER_RELEASE/piper_linux_armv7l.tar.gz\" | tar -xz -C \"$PIPER_DIR\" --strip-components=1\n" +
            "    fi\n" +
            "    chmod +x \"$PIPER_DIR/piper\" 2>/dev/null\n" +
            "    export PATH=\"$PIPER_DIR:$PATH\"\n" +
            "  fi\n" +
            "else\n" +
            "  [ -f \"$PIPER_DIR/piper\" ] && export PATH=\"$PIPER_DIR:$PATH\"\n" +
            "fi\n\n" +
            "# Determine piper command\n" +
            "if command -v piper &>/dev/null; then\n" +
            "  PIPER_CMD=\"piper\"\n" +
            "elif python3 -c 'import piper' &>/dev/null 2>&1; then\n" +
            "  PIPER_CMD=\"python3 -m piper\"\n" +
            "else\n" +
            "  echo -e \"\\033[1;31m✗ Piper not available. Run: pip3 install piper-tts\\033[0m\"\n" +
            "  exit 1\n" +
            "fi\n\n" +
            "# Step 2 — ensure voice model\n" +
            "if [ ! -f \"$VOICE_MODEL\" ]; then\n" +
            "  echo -e \"\\033[1;33m⬡ Downloading voice model (en_US Lessac)…\\033[0m\"\n" +
            "  mkdir -p \"$VOICE_DIR\"\n" +
            "  curl -sL --progress-bar \"$VOICE_URL/en_US-lessac-medium.onnx\" -o \"$VOICE_MODEL\"\n" +
            "  curl -sL \"$VOICE_URL/en_US-lessac-medium.onnx.json\" -o \"$VOICE_CONFIG\"\n" +
            "  echo -e \"\\033[1;32m✓ Voice model ready\\033[0m\"\n" +
            "fi\n\n" +
            "# Step 3 — synthesize and play\n" +
            "echo -e \"\\033[1;36m⬡ Speaking…\\033[0m\"\n" +
            "echo \"$TEXT\" | $PIPER_CMD --model \"$VOICE_MODEL\" --output_file \"$TMP_WAV\" 2>/dev/null\n\n" +
            "# Play with whatever is available on Termux\n" +
            "if command -v termux-media-player &>/dev/null; then\n" +
            "  termux-media-player play \"$TMP_WAV\"\n" +
            "  sleep 0.5\n" +
            "  # Wait for playback\n" +
            "  while termux-media-player info 2>/dev/null | grep -q '\"status\": \"playing\"'; do sleep 0.3; done\n" +
            "elif command -v mpv &>/dev/null; then\n" +
            "  mpv --no-terminal \"$TMP_WAV\" 2>/dev/null\n" +
            "elif command -v aplay &>/dev/null; then\n" +
            "  aplay \"$TMP_WAV\" 2>/dev/null\n" +
            "else\n" +
            "  echo -e \"\\033[1;33m✓ Audio saved to $TMP_WAV (install mpv or termux-api to auto-play)\\033[0m\"\n" +
            "fi\n";

        try {
            File speakFile = new File(dir, "nastech_speak.sh");
            try (FileOutputStream fos = new FileOutputStream(speakFile)) {
                fos.write(speakScript.getBytes());
            }
            // Make executable
            speakFile.setExecutable(true, false);
        } catch (IOException ignored) {}
    }

    private static byte[] readFile(File f) throws IOException {
        java.io.FileInputStream fis = new java.io.FileInputStream(f);
        byte[] data = new byte[(int) f.length()];
        fis.read(data);
        fis.close();
        return data;
    }

    public static SharedPreferences getPrefs() {
        return sAppContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static String getApiKey() {
        return getPrefs().getString(KEY_API_KEY, "");
    }

    public static void setApiKey(String key) {
        getPrefs().edit().putString(KEY_API_KEY, key).apply();
    }

    public static boolean isBiometricLockEnabled() {
        return getPrefs().getBoolean(KEY_BIOMETRIC, false);
    }

    public static void setBiometricLock(boolean enabled) {
        getPrefs().edit().putBoolean(KEY_BIOMETRIC, enabled).apply();
    }
}
