package com.enext.biometria;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONObject;

/**
 * Activity que muestra la interfaz de validacion biometrica.
 * Carga un WebView con la UI de captura de foto y validacion.
 * El HTML se carga directamente desde el codigo para evitar problemas de archivos externos.
 */
public class BiometriaActivity extends Activity {

    private static final String TAG = "BiometriaActivity";
    private static final int CAMERA_PERMISSION_REQUEST = 1001;
    
    // WebView que contiene la interfaz de usuario
    private WebView webView;
    
    // Datos recibidos del plugin principal
    private String cedula;
    private String codDactilar;
    private String tokenEndpoint;
    private String biometriaEndpoint;
    private String username;
    private String password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Configurar pantalla completa sin barra de titulo
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        
        // Obtener datos del Intent enviados por el plugin principal
        Intent intent = getIntent();
        cedula = intent.getStringExtra("cedula");
        codDactilar = intent.getStringExtra("codDactilar");
        tokenEndpoint = intent.getStringExtra("tokenEndpoint");
        biometriaEndpoint = intent.getStringExtra("biometriaEndpoint");
        username = intent.getStringExtra("username");
        password = intent.getStringExtra("password");
        
        Log.d(TAG, "BiometriaActivity iniciada para cedula: " + cedula);
        
        // Verificar permiso de camara antes de iniciar
        if (checkCameraPermission()) {
            initializeWebView();
        } else {
            requestCameraPermission();
        }
    }
    
    /**
     * Verifica si el permiso de camara esta concedido
     */
    private boolean checkCameraPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }
    
    /**
     * Solicita el permiso de camara al usuario
     */
    private void requestCameraPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.CAMERA}, 
                CAMERA_PERMISSION_REQUEST);
        }
    }
    
    /**
     * Callback del resultado de la solicitud de permisos
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permiso de camara concedido");
                initializeWebView();
            } else {
                Log.e(TAG, "Permiso de camara denegado");
                // Enviar error al plugin y cerrar
                Intent resultIntent = new Intent();
                resultIntent.putExtra("errorCode", "CAMERA_PERMISSION_DENIED");
                resultIntent.putExtra("errorMessage", "Se requiere permiso de camara");
                setResult(Activity.RESULT_CANCELED, resultIntent);
                finish();
            }
        }
    }
    
    /**
     * Inicializa el WebView con la interfaz de usuario
     */
    private void initializeWebView() {
        webView = new WebView(this);
        setContentView(webView);
        setupWebView();
        
        // Cargar el contenido HTML directamente (no desde archivo externo)
        String htmlContent = getHtmlContent();
        webView.loadDataWithBaseURL("file:///android_asset/", htmlContent, "text/html", "UTF-8", null);
    }

    /**
     * Configura las opciones del WebView
     */
    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        
        // Habilitar JavaScript
        settings.setJavaScriptEnabled(true);
        
        // Permitir acceso a archivos locales
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        
        // Configuraciones adicionales
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setDatabaseEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        
        // Agregar interfaz JavaScript para comunicacion con Android
        webView.addJavascriptInterface(new BiometriaJSInterface(), "AndroidBiometria");
        
        // Configurar WebViewClient
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.d(TAG, "Pagina cargada");
                // Inyectar la configuracion en el JavaScript
                injectConfig();
            }
        });
        
        // Configurar WebChromeClient para permisos de camara en el WebView
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                Log.d(TAG, "WebView solicita permiso de camara");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Conceder permisos al WebView
                        request.grant(request.getResources());
                    }
                });
            }
        });
    }

    /**
     * Inyecta la configuracion en el JavaScript del WebView
     */
    private void injectConfig() {
        String js = String.format(
            "window.BIOMETRIA_CONFIG = {" +
            "  cedula: '%s'," +
            "  codDactilar: '%s'," +
            "  tokenEndpoint: '%s'," +
            "  biometriaEndpoint: '%s'," +
            "  username: '%s'," +
            "  password: '%s'" +
            "}; if(typeof onConfigReady === 'function') onConfigReady();",
            escapeJS(cedula),
            escapeJS(codDactilar),
            escapeJS(tokenEndpoint),
            escapeJS(biometriaEndpoint),
            escapeJS(username),
            escapeJS(password)
        );
        webView.evaluateJavascript(js, null);
    }

    /**
     * Escapa caracteres especiales para JavaScript
     */
    private String escapeJS(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("'", "\\'")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r");
    }

    /**
     * Retorna el contenido HTML completo de la interfaz de usuario.
     * Incluye CSS y JavaScript inline para evitar dependencias de archivos externos.
     */
    private String getHtmlContent() {
        return "<!DOCTYPE html><html><head>" +
            "<meta charset=\"utf-8\">" +
            "<meta name=\"viewport\" content=\"initial-scale=1, width=device-width, viewport-fit=cover, user-scalable=no\">" +
            "<meta http-equiv=\"Content-Security-Policy\" content=\"default-src * 'unsafe-inline' 'unsafe-eval' data: blob:;\">" +
            "<title>Validacion Biometrica</title>" +
            "<style>" +
            // Estilos CSS
            "*{box-sizing:border-box}body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;margin:0;padding:0;height:100vh;width:100vw;overflow:hidden;background:#000;color:#333}" +
            ".page{display:none;position:absolute;top:0;left:0;width:100%;height:100%;background:#f0f2f5}.page.active{display:flex;flex-direction:column;justify-content:center;align-items:center}" +
            ".content{text-align:center;padding:20px}button{padding:15px;border:none;border-radius:8px;background:#007aff;color:#fff;font-size:1.1rem;font-weight:bold;cursor:pointer}" +
            "button:disabled{background:#999}#cameraPage{background:#000}" +
            ".branding-bg{width:100%;height:100%;background:linear-gradient(180deg,#1a237e 0%,#0d47a1 30%,#1565c0 60%,#1976d2 100%);position:relative;display:flex;flex-direction:column;justify-content:center;align-items:center;padding:20px;box-sizing:border-box}" +
            "#closeCameraBtn{position:absolute;top:15px;left:15px;z-index:20;background:rgba(0,0,0,0.3);border-radius:50%;width:44px;height:44px;font-size:1.5rem;display:flex;align-items:center;justify-content:center;border:none;color:#fff}" +
            ".camera-title{color:#fff;font-size:1.5rem;font-weight:600;text-shadow:1px 1px 3px rgba(0,0,0,0.4);margin:0 0 15px}" +
            ".camera-circle{position:relative;width:min(80vw,340px);height:min(80vw,340px);margin:0 auto 10px;border-radius:50%;overflow:hidden;border:4px solid #ffd800;background:#000;display:flex;align-items:center;justify-content:center}" +
            ".camera-circle::before{content:'';position:absolute;top:-4px;left:-4px;right:-4px;bottom:-4px;border-radius:50%;background:conic-gradient(transparent 0deg,transparent 0deg);z-index:1}" +
            ".camera-circle.timer-active::before{animation:draw-border 3s linear forwards}" +
            "@keyframes draw-border{0%{background:conic-gradient(orange 0deg,transparent 0deg)}100%{background:conic-gradient(orange 360deg,transparent 0deg)}}" +
            ".camera-circle video{width:100%;height:100%;object-fit:cover;z-index:2;position:relative;transform:scaleX(-1)}" +
            ".camera-bottom-container{height:70px;width:100%;display:flex;align-items:center;justify-content:center;flex-direction:column}" +
            ".camera-subtitle{color:#fff;font-size:1rem;margin:0}" +
            "#capture-progress-container{text-align:center;color:#fff;width:100%;display:none}" +
            "#capture-progress-container p{font-size:1rem;margin-bottom:10px}" +
            ".progress-bar-outline{width:80%;max-width:300px;height:10px;border:1px solid #fff;border-radius:5px;margin:0 auto;overflow:hidden}" +
            ".progress-bar-inner{height:100%;width:0;background:#3498db;border-radius:5px}.progress-bar-inner.animating{transition:width 3s linear}" +
            ".camera-controls{display:flex;justify-content:center;margin-top:15px}" +
            "#startCaptureBtn{width:auto;padding:12px 40px;font-size:1.2rem;font-weight:600;border-radius:30px;border:2px solid #fff;background:#3498db;box-shadow:0 4px 8px rgba(0,0,0,0.2)}" +
            "#startCaptureBtn:disabled{background:#95a5a6}" +
            "#retry-container{display:none;flex-direction:column;align-items:center;margin-top:15px}" +
            "#retryBtn{padding:12px 28px;background:#d35400;color:#fff;border-radius:6px;border:none;width:auto}" +
            ".popup{display:none;position:absolute;bottom:50%;left:50%;transform:translate(-50%,200%);background:rgba(0,0,0,0.8);color:#fff;padding:15px 25px;border-radius:10px;z-index:100;text-align:center;flex-direction:column}" +
            ".spinner{display:none;margin:10px auto 0;border:4px solid rgba(255,255,255,0.3);border-radius:50%;border-top:4px solid #fff;width:30px;height:30px;animation:spin 1s linear infinite}" +
            "@keyframes spin{0%{transform:rotate(0deg)}100%{transform:rotate(360deg)}}" +
            ".result-page{display:flex;flex-direction:column;justify-content:center;align-items:center;height:100%;background:#f0f2f5}" +
            ".result-page h1{font-size:1.8rem;margin:10px 0 5px;color:#333}.result-page p{margin-bottom:10px;color:#666}" +
            ".result-icon{font-size:40px;width:70px;height:70px;line-height:70px;border-radius:50%;display:flex;align-items:center;justify-content:center}" +
            ".result-icon.success{background:#34c759;color:#fff}.result-icon.failure{background:#ff3b30;color:#fff}" +
            ".go-home-btn{margin-top:20px;width:auto;padding:15px 40px}" +
            ".token-container{word-wrap:break-word;background:#f0f0f0;border-radius:8px;padding:10px 15px;margin:10px 0;width:90%;max-width:500px;text-align:left;color:#333}" +
            ".token-container p{margin:0 0 5px;font-size:0.9rem;font-weight:600}" +
            ".token-container code{display:block;background:#e0e0e0;padding:8px;border-radius:4px;font-family:monospace;font-size:0.75rem;word-break:break-all;max-height:80px;overflow-y:auto}" +
            ".button-group{display:flex;gap:10px;justify-content:center;width:90%;max-width:500px;margin-top:15px}.button-group button{flex:1}" +
            ".biometric-data{display:grid;grid-template-columns:auto 1fr;gap:5px 12px;padding-top:5px;font-size:0.8rem}" +
            ".biometric-data strong{text-transform:capitalize;color:#555;font-weight:600}.biometric-data span{color:#111}" +
            "#loadingPage{background:linear-gradient(180deg,#1a237e 0%,#0d47a1 30%,#1565c0 60%,#1976d2 100%)}#loadingPage .content{color:#fff}" +
            ".loading-spinner{border:4px solid rgba(255,255,255,0.3);border-radius:50%;border-top:4px solid #ffd800;width:50px;height:50px;animation:spin 1s linear infinite;margin:20px auto}" +
            "</style></head><body>" +
            // HTML de la interfaz
            "<div id=\"loadingPage\" class=\"page active\"><div class=\"content\"><div class=\"loading-spinner\"></div><h2>Iniciando...</h2><p id=\"loadingMessage\">Generando token...</p></div></div>" +
            "<div id=\"cameraPage\" class=\"page\"><div class=\"branding-bg\"><button id=\"closeCameraBtn\">←</button><h2 class=\"camera-title\">Valida tu identidad</h2>" +
            "<div class=\"camera-circle\" id=\"cameraCircle\"><video id=\"cameraPreview\" autoplay playsinline muted></video></div>" +
            "<div class=\"camera-bottom-container\"><p class=\"camera-subtitle\" id=\"cameraSubtitle\">Coloca tu rostro en el circulo</p>" +
            "<div id=\"capture-progress-container\"><p>Capturando rostro...</p><div class=\"progress-bar-outline\"><div id=\"capture-progress-bar\" class=\"progress-bar-inner\"></div></div></div></div>" +
            "<div class=\"camera-controls\" id=\"cameraControls\"><button id=\"startCaptureBtn\">Validar</button></div>" +
            "<div id=\"camera-popup\" class=\"popup\"><p id=\"camera-popup-message\"></p><div class=\"spinner\" id=\"popupSpinner\"></div></div>" +
            "<div id=\"retry-container\"><button id=\"retryBtn\">Intentar de Nuevo</button></div></div></div>" +
            "<div id=\"successPage\" class=\"page\"><div class=\"content result-page\"><div class=\"result-icon success\">OK</div><h1>Validacion Exitosa</h1><p>El proceso se completo correctamente.</p>" +
            "<div class=\"token-container\" id=\"biometricDataContainer\" style=\"display:none\"><p>Datos del Ciudadano:</p><div id=\"biometricDataDisplay\" class=\"biometric-data\"></div></div>" +
            "<div class=\"token-container\"><p>Access Token:</p><code id=\"accessTokenDisplay\">(Token)</code></div>" +
            "<div class=\"button-group\"><button class=\"go-home-btn\" id=\"successBtn\">Continuar</button></div></div></div>" +
            "<div id=\"failurePage\" class=\"page\"><div class=\"content result-page\"><div class=\"result-icon failure\">X</div><h1>Validacion Fallida</h1><p id=\"failureMessage\">No se pudo completar.</p>" +
            "<div class=\"button-group\"><button class=\"go-home-btn\" id=\"retryValidationBtn\">Reintentar</button><button class=\"go-home-btn\" id=\"cancelBtn\" style=\"background:#ff3b30\">Cancelar</button></div></div></div>" +
            "<script>" +
            // JavaScript de la logica
            "var config=null,validationAttempts=0,MAX_ATTEMPTS=3,cameraStream=null,accessToken=null,pages={},cameraPage={};" +
            "document.addEventListener('DOMContentLoaded',function(){initDOMElements();setTimeout(tryGetConfig,100)});" +
            "function onConfigReady(){if(window.BIOMETRIA_CONFIG){config=window.BIOMETRIA_CONFIG;startProcess()}}" +
            "function tryGetConfig(){if(window.AndroidBiometria){try{var c=AndroidBiometria.getConfig();config=JSON.parse(c);startProcess();return}catch(e){}}if(window.BIOMETRIA_CONFIG){config=window.BIOMETRIA_CONFIG;startProcess();return}setTimeout(tryGetConfig,200)}" +
            "function initDOMElements(){pages.loading=document.getElementById('loadingPage');pages.camera=document.getElementById('cameraPage');pages.success=document.getElementById('successPage');pages.failure=document.getElementById('failurePage');" +
            "cameraPage.video=document.getElementById('cameraPreview');cameraPage.closeBtn=document.getElementById('closeCameraBtn');cameraPage.popup=document.getElementById('camera-popup');cameraPage.popupMessage=document.getElementById('camera-popup-message');" +
            "cameraPage.spinner=document.getElementById('popupSpinner');cameraPage.retryContainer=document.getElementById('retry-container');cameraPage.retryBtn=document.getElementById('retryBtn');cameraPage.startCaptureBtn=document.getElementById('startCaptureBtn');" +
            "cameraPage.cameraControls=document.getElementById('cameraControls');cameraPage.progressContainer=document.getElementById('capture-progress-container');cameraPage.progressBar=document.getElementById('capture-progress-bar');" +
            "cameraPage.subtitle=document.getElementById('cameraSubtitle');cameraPage.cameraCircle=document.getElementById('cameraCircle');" +
            "if(cameraPage.closeBtn)cameraPage.closeBtn.addEventListener('click',cancelValidation);if(cameraPage.startCaptureBtn)cameraPage.startCaptureBtn.addEventListener('click',runCaptureSequence);if(cameraPage.retryBtn)cameraPage.retryBtn.addEventListener('click',runCaptureSequence);" +
            "var s=document.getElementById('successBtn');if(s)s.addEventListener('click',completeSuccess);var r=document.getElementById('retryValidationBtn');if(r)r.addEventListener('click',retryFromFailure);var c=document.getElementById('cancelBtn');if(c)c.addEventListener('click',cancelValidation)}" +
            "function startProcess(){if(!config){sendError('CONFIG_ERROR','No config');return}showPage('loading');updateLoadingMessage('Generando token...');fetchAuthToken()}" +
            "function fetchAuthToken(){var xhr=new XMLHttpRequest();xhr.open('POST',config.tokenEndpoint,true);xhr.setRequestHeader('Content-Type','application/json');" +
            "xhr.onreadystatechange=function(){if(xhr.readyState===4){if(xhr.status===200){try{var d=JSON.parse(xhr.responseText);if(d.access_token){accessToken=d.access_token;updateLoadingMessage('Iniciando camara...');setTimeout(startCamera,500)}else{sendError('TOKEN_ERROR','Token invalido')}}catch(e){sendError('TOKEN_PARSE_ERROR','Respuesta invalida')}}else{sendError('TOKEN_REQUEST_ERROR','No se pudo obtener token')}}};" +
            "xhr.onerror=function(){sendError('TOKEN_NETWORK_ERROR','Error de red')};xhr.send(JSON.stringify({username:config.username,password:config.password}))}" +
            "function startCamera(){showPage('camera');if(cameraStream)stopCamera();cameraPage.startCaptureBtn.style.display='block';cameraPage.startCaptureBtn.disabled=false;cameraPage.retryContainer.style.display='none';cameraPage.progressContainer.style.display='none';cameraPage.subtitle.style.display='block';cameraPage.cameraControls.style.display='flex';hideCameraPopup();" +
            "navigator.mediaDevices.getUserMedia({video:{facingMode:'user',width:{ideal:1280},height:{ideal:720}},audio:false}).then(function(s){cameraStream=s;cameraPage.video.srcObject=s}).catch(function(e){sendError('CAMERA_ERROR','No se pudo acceder a la camara')})}" +
            "function stopCamera(){if(cameraStream){cameraStream.getTracks().forEach(function(t){t.stop()});cameraStream=null}}" +
            "function runCaptureSequence(){cameraPage.startCaptureBtn.style.display='none';cameraPage.cameraControls.style.display='none';cameraPage.retryContainer.style.display='none';cameraPage.subtitle.style.display='none';cameraPage.progressContainer.style.display='block';" +
            "cameraPage.progressBar.classList.remove('animating');cameraPage.progressBar.style.width='0%';void cameraPage.progressBar.offsetWidth;cameraPage.progressBar.classList.add('animating');cameraPage.progressBar.style.width='100%';" +
            "cameraPage.cameraCircle.classList.remove('timer-active');void cameraPage.cameraCircle.offsetWidth;cameraPage.cameraCircle.classList.add('timer-active');" +
            "setTimeout(function(){cameraPage.progressContainer.style.display='none';cameraPage.progressBar.classList.remove('animating');cameraPage.progressBar.style.width='0%';cameraPage.cameraCircle.classList.remove('timer-active');takeAndProcessPhoto()},3000)}" +
            "function takeAndProcessPhoto(){showCameraPopup('Validando foto...',true);var v=cameraPage.video;if(!v.srcObject){handleApiFailure('Camara no activa');return}" +
            "var c=document.createElement('canvas');c.width=v.videoWidth;c.height=v.videoHeight;var ctx=c.getContext('2d');ctx.drawImage(v,0,0,c.width,c.height);var b64=c.toDataURL('image/jpeg',0.85);validationAttempts++;" +
            "var xhr=new XMLHttpRequest();xhr.open('POST',config.biometriaEndpoint,true);xhr.setRequestHeader('Content-Type','application/json');" +
            "xhr.onreadystatechange=function(){if(xhr.readyState===4){if(xhr.status===200){try{var r=JSON.parse(xhr.responseText);if(r.status==='success'){handleBiometricSuccess(r)}else{handleApiFailure(r.message||'Validacion fallida')}}catch(e){handleApiFailure('Respuesta invalida')}}else{var msg='Error del servidor';try{var er=JSON.parse(xhr.responseText);if(er&&er.message)msg=er.message}catch(e){}handleApiFailure(msg)}}};" +
            "xhr.onerror=function(){handleApiFailure('Error de red')};xhr.send(JSON.stringify({cedula:config.cedula,cod_dactilar:config.codDactilar,img_base64:b64.split(',')[1],token:accessToken}))}" +
            "function handleBiometricSuccess(data){stopCamera();var bd=data.datos_RC||data;document.getElementById('accessTokenDisplay').textContent=accessToken;" +
            "var cont=document.getElementById('biometricDataContainer'),disp=document.getElementById('biometricDataDisplay');if(bd&&typeof bd==='object'){var h='';for(var k in bd){if(bd.hasOwnProperty(k)){var fk=k.replace(/([A-Z])/g,' $1');fk=fk.charAt(0).toUpperCase()+fk.slice(1);h+='<strong>'+fk+':</strong> <span>'+bd[k]+'</span>'}}if(h){disp.innerHTML=h;cont.style.display='block'}}" +
            "showPage('success');window.validationResult={accessToken:accessToken,biometricData:bd,timestamp:new Date().toISOString()}}" +
            "function completeSuccess(){if(window.AndroidBiometria&&window.validationResult){AndroidBiometria.onSuccess(window.validationResult.accessToken,JSON.stringify(window.validationResult.biometricData),window.validationResult.timestamp)}}" +
            "function handleApiFailure(msg){hideCameraPopup();if(validationAttempts>=MAX_ATTEMPTS){stopCamera();document.getElementById('failureMessage').textContent=msg;showPage('failure')}else{showCameraPopup(msg+'\\nIntento '+validationAttempts+'/'+MAX_ATTEMPTS,false);setTimeout(function(){hideCameraPopup();cameraPage.retryContainer.style.display='flex';cameraPage.startCaptureBtn.style.display='none';cameraPage.cameraControls.style.display='none';cameraPage.progressContainer.style.display='none';cameraPage.subtitle.style.display='block'},4000)}}" +
            "function retryFromFailure(){validationAttempts=0;showPage('loading');updateLoadingMessage('Reiniciando...');setTimeout(startCamera,500)}" +
            "function cancelValidation(){stopCamera();if(window.AndroidBiometria)AndroidBiometria.onCancel()}" +
            "function sendError(code,msg){stopCamera();if(window.AndroidBiometria)AndroidBiometria.onError(code,msg)}" +
            "function showPage(id){for(var k in pages){if(pages[k])pages[k].classList.remove('active')}if(pages[id])pages[id].classList.add('active')}" +
            "function updateLoadingMessage(msg){var el=document.getElementById('loadingMessage');if(el)el.textContent=msg}" +
            "function showCameraPopup(msg,spin){cameraPage.popupMessage.textContent=msg;cameraPage.spinner.style.display=spin?'block':'none';cameraPage.popup.style.display='flex'}" +
            "function hideCameraPopup(){if(cameraPage.popup)cameraPage.popup.style.display='none'}" +
            "</script></body></html>";
    }

    /**
     * Interfaz JavaScript para comunicacion entre el WebView y Android.
     * Permite que el JavaScript llame metodos de Android.
     */
    public class BiometriaJSInterface {
        
        /**
         * Llamado cuando la validacion biometrica es exitosa
         */
        @JavascriptInterface
        public void onSuccess(String accessToken, String biometricData, String timestamp) {
            Log.d(TAG, "Validacion exitosa");
            Intent resultIntent = new Intent();
            resultIntent.putExtra("accessToken", accessToken);
            resultIntent.putExtra("biometricData", biometricData);
            resultIntent.putExtra("timestamp", timestamp);
            setResult(Activity.RESULT_OK, resultIntent);
            finish();
        }
        
        /**
         * Llamado cuando ocurre un error en la validacion
         */
        @JavascriptInterface
        public void onError(String errorCode, String errorMessage) {
            Log.d(TAG, "Error: " + errorCode + " - " + errorMessage);
            Intent resultIntent = new Intent();
            resultIntent.putExtra("errorCode", errorCode);
            resultIntent.putExtra("errorMessage", errorMessage);
            setResult(Activity.RESULT_CANCELED, resultIntent);
            finish();
        }
        
        /**
         * Llamado cuando el usuario cancela la validacion
         */
        @JavascriptInterface
        public void onCancel() {
            Log.d(TAG, "Validacion cancelada por el usuario");
            setResult(Activity.RESULT_CANCELED);
            finish();
        }
        
        /**
         * Retorna la configuracion como JSON para el JavaScript
         */
        @JavascriptInterface
        public String getConfig() {
            try {
                JSONObject config = new JSONObject();
                config.put("cedula", cedula);
                config.put("codDactilar", codDactilar);
                config.put("tokenEndpoint", tokenEndpoint);
                config.put("biometriaEndpoint", biometriaEndpoint);
                config.put("username", username);
                config.put("password", password);
                return config.toString();
            } catch (Exception e) {
                Log.e(TAG, "Error al crear configuracion JSON: " + e.getMessage());
                return "{}";
            }
        }
    }

    /**
     * Maneja el boton de retroceso del dispositivo
     */
    @Override
    public void onBackPressed() {
        setResult(Activity.RESULT_CANCELED);
        super.onBackPressed();
    }

    /**
     * Limpia recursos al destruir la actividad
     */
    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }
}
