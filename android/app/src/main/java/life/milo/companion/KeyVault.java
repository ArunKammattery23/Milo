package life.milo.companion;
import android.content.*;
import android.security.keystore.*;
import android.util.Base64;
import java.security.KeyStore;
import java.nio.charset.StandardCharsets;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
final class KeyVault {
    static javax.crypto.SecretKey secret()throws Exception{KeyStore s=KeyStore.getInstance("AndroidKeyStore");s.load(null);if(!s.containsAlias(MainActivity.KEY_ALIAS)){KeyGenerator gen=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");gen.init(new KeyGenParameterSpec.Builder(MainActivity.KEY_ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());gen.generateKey();}return (javax.crypto.SecretKey)s.getKey(MainActivity.KEY_ALIAS,null);}
    static String load(Context c)throws Exception{SharedPreferences p=c.getSharedPreferences("milo",Context.MODE_PRIVATE);String data=p.getString("key_data","");if(data.isEmpty())return "";Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,secret(),new GCMParameterSpec(128,Base64.decode(p.getString("key_iv",""),Base64.NO_WRAP)));return new String(cipher.doFinal(Base64.decode(data,Base64.NO_WRAP)),StandardCharsets.UTF_8);}
    static void save(Context c,String key)throws Exception{Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,secret());byte[] data=cipher.doFinal(key.getBytes(StandardCharsets.UTF_8));if(!c.getSharedPreferences("milo",Context.MODE_PRIVATE).edit().putString("key_iv",Base64.encodeToString(cipher.getIV(),Base64.NO_WRAP)).putString("key_data",Base64.encodeToString(data,Base64.NO_WRAP)).commit())throw new java.io.IOException("Could not save encrypted key");}

    /* ---- per-provider key storage ---- */

    /** Load key for a specific AI provider. Falls back to legacy key for Gemini. */
    static String loadForProvider(Context c, String providerId) throws Exception {
        if (AiProvider.LOCAL.equals(providerId)) return "";  // local needs no key
        SharedPreferences p = c.getSharedPreferences("milo", Context.MODE_PRIVATE);
        String suffix = "_" + providerId;
        String data = p.getString("key_data" + suffix, "");
        if (data.isEmpty()) {
            // Fall back to legacy key_data for Gemini (backward compatibility)
            if (AiProvider.GEMINI.equals(providerId)) return load(c);
            return "";
        }
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, secret(),
                new GCMParameterSpec(128, Base64.decode(p.getString("key_iv" + suffix, ""), Base64.NO_WRAP)));
        return new String(cipher.doFinal(Base64.decode(data, Base64.NO_WRAP)), StandardCharsets.UTF_8);
    }

    /** Save key for a specific AI provider. */
    static void saveForProvider(Context c, String providerId, String key) throws Exception {
        if (AiProvider.LOCAL.equals(providerId)) return;  // nothing to save
        // Also save as legacy key if Gemini (backward compat)
        if (AiProvider.GEMINI.equals(providerId)) save(c, key);
        String suffix = "_" + providerId;
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, secret());
        byte[] data = cipher.doFinal(key.getBytes(StandardCharsets.UTF_8));
        if (!c.getSharedPreferences("milo", Context.MODE_PRIVATE).edit()
                .putString("key_iv" + suffix, Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP))
                .putString("key_data" + suffix, Base64.encodeToString(data, Base64.NO_WRAP))
                .commit())
            throw new java.io.IOException("Could not save encrypted key");
    }

    /** Remove key for a specific provider. */
    static void removeForProvider(Context c, String providerId) {
        String suffix = "_" + providerId;
        c.getSharedPreferences("milo", Context.MODE_PRIVATE).edit()
                .remove("key_iv" + suffix).remove("key_data" + suffix).apply();
        // Also remove legacy key if Gemini
        if (AiProvider.GEMINI.equals(providerId)) {
            c.getSharedPreferences("milo", Context.MODE_PRIVATE).edit()
                    .remove("key_iv").remove("key_data").apply();
        }
    }

    /** Check if a provider has a stored key. */
    static boolean hasKeyForProvider(Context c, String providerId) {
        if (AiProvider.LOCAL.equals(providerId)) return true;  // local never needs a key
        try {
            return !loadForProvider(c, providerId).isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
}
