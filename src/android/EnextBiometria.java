package com.enext.biometria;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Plugin principal de Enext Biometria para Cordova.
 * Permite la validacion biometrica facial en aplicaciones Cordova.
 */
public class EnextBiometria extends CordovaPlugin {

    private static final String TAG = "EnextBiometria";
    private static final int REQUEST_CODE_BIOMETRIA = 1001;
    
    // Callback para retornar resultado al JavaScript
    private CallbackContext callbackContext;

    /**
     * Metodo principal que ejecuta las acciones del plugin.
     */
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext;

        if ("validar".equals(action)) {
            return validarBiometria(args);
        } else if ("cancelar".equals(action)) {
            return cancelarValidacion();
        } else if ("configurar".equals(action)) {
            callbackContext.success();
            return true;
        }

        return false;
    }

    /**
     * Inicia el proceso de validacion biometrica.
     */
    private boolean validarBiometria(JSONArray args) throws JSONException {
        String cedula = args.getString(0);
        String codDactilar = args.getString(1);
        String tokenEndpoint = args.getString(2);
        String biometriaEndpoint = args.getString(3);
        String username = args.getString(4);
        String password = args.getString(5);

        Log.d(TAG, "Iniciando validacion para cedula: " + cedula);

        Intent intent = new Intent(cordova.getActivity(), BiometriaActivity.class);
        intent.putExtra("cedula", cedula);
        intent.putExtra("codDactilar", codDactilar);
        intent.putExtra("tokenEndpoint", tokenEndpoint);
        intent.putExtra("biometriaEndpoint", biometriaEndpoint);
        intent.putExtra("username", username);
        intent.putExtra("password", password);

        cordova.startActivityForResult(this, intent, REQUEST_CODE_BIOMETRIA);

        PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
        pluginResult.setKeepCallback(true);
        callbackContext.sendPluginResult(pluginResult);

        return true;
    }

    /**
     * Cancela la validacion en curso.
     */
    private boolean cancelarValidacion() {
        callbackContext.success();
        return true;
    }

    /**
     * Procesa el resultado de BiometriaActivity.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (requestCode == REQUEST_CODE_BIOMETRIA) {
            if (resultCode == Activity.RESULT_OK) {
                handleSuccess(intent);
            } else {
                handleError(intent);
            }
        }
    }

    /**
     * Maneja el resultado exitoso de la validacion.
     */
    private void handleSuccess(Intent intent) {
        try {
            JSONObject result = new JSONObject();
            result.put("accessToken", intent.getStringExtra("accessToken"));
            
            String biometricDataStr = intent.getStringExtra("biometricData");
            if (biometricDataStr != null && !biometricDataStr.isEmpty()) {
                try {
                    JSONObject biometricData = new JSONObject(biometricDataStr);
                    result.put("biometricData", biometricData);
                } catch (JSONException e) {
                    result.put("biometricData", biometricDataStr);
                }
            }
            
            result.put("timestamp", intent.getStringExtra("timestamp"));

            Log.d(TAG, "Validacion exitosa");
            callbackContext.success(result);
            
        } catch (JSONException e) {
            Log.e(TAG, "Error al crear JSON: " + e.getMessage());
            sendError("PARSE_ERROR", "Error al procesar resultado");
        }
    }

    /**
     * Maneja el resultado de error o cancelacion.
     */
    private void handleError(Intent intent) {
        if (intent != null) {
            String errorCode = intent.getStringExtra("errorCode");
            String errorMessage = intent.getStringExtra("errorMessage");
            
            if (errorCode != null) {
                Log.d(TAG, "Error: " + errorCode + " - " + errorMessage);
                sendError(errorCode, errorMessage != null ? errorMessage : "Error en validacion");
                return;
            }
        }
        
        Log.d(TAG, "Validacion cancelada");
        sendError("CANCELLED", "Validacion cancelada");
    }

    /**
     * Envia un error al callback de JavaScript.
     */
    private void sendError(String code, String message) {
        try {
            JSONObject error = new JSONObject();
            error.put("code", code);
            error.put("message", message);
            callbackContext.error(error);
        } catch (JSONException e) {
            callbackContext.error("Error desconocido");
        }
    }
}
