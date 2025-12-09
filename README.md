# Cordova Plugin Enext Biometria

Plugin de Cordova para validacion biometrica facial de Enext.

## Instalacion

### Desde carpeta local:
```bash
cordova plugin add /ruta/a/cordova-plugin-enext-biometria
```

### Desde repositorio Git:
```bash
cordova plugin add https://github.com/enext/cordova-plugin-enext-biometria.git
```

## Requisitos

- Cordova >= 9.0.0
- Android >= 6.0 (API 23)
- El plugin `cordova-plugin-advanced-http` se instala automaticamente como dependencia
- Credenciales proporcionadas por Enext (username y password)

## Integracion Rapida

### Paso 1: Configurar Credenciales al Iniciar la App

Las credenciales se configuran en el codigo, no hay pantalla de login.
Debe llamar a `configurar()` una sola vez cuando la app inicia:

```javascript
document.addEventListener('deviceready', function() {
    
    // Configurar credenciales proporcionadas por Enext
    EnextBiometria.configurar({
        credentials: {
            username: 'su_usuario',    // Proporcionado por Enext
            password: 'su_password'    // Proporcionado por Enext
        }
    });
    
}, false);
```

### Paso 2: Validar Identidad Cuando el Usuario lo Requiera

Cuando el usuario necesite validar su identidad, llame a `validar()`:

```javascript
function validarIdentidad() {
    var cedula = document.getElementById('inputCedula').value;
    var codDactilar = document.getElementById('inputCodDactilar').value;
    
    EnextBiometria.validar(
        {
            cedula: cedula,
            codDactilar: codDactilar
        },
        function(resultado) {
            // EXITO
            console.log('Token:', resultado.accessToken);
            console.log('Datos:', resultado.biometricData);
        },
        function(error) {
            // ERROR
            if (error.code !== 'CANCELLED') {
                alert('Error: ' + error.message);
            }
        }
    );
}
```

## Flujo de la Aplicacion

```
1. La app inicia
        |
        v
2. deviceready -> EnextBiometria.configurar({ credentials: {...} })
        |
        v
3. El usuario ingresa cedula y codigo dactilar en su formulario
        |
        v
4. El usuario presiona un boton para validar
        |
        v
5. EnextBiometria.validar({...})
        |
        v
6. Se abre la camara en pantalla completa
        |
        v
7. El usuario presiona "Validar" en la pantalla de camara
        |
        v
8. Barra de progreso de 3 segundos
        |
        v
9. Se captura la foto automaticamente
        |
        v
10. Se valida con el servidor de biometria
        |
        v
11. Retorna resultado (exito o error)
```

## Configuracion Completa

Si necesita cambiar los endpoints (opcional):

```javascript
EnextBiometria.configurar({
    tokenEndpoint: 'https://otro-servidor.com/token',           // Opcional
    biometriaEndpoint: 'https://otro-servidor.com/validar',     // Opcional
    credentials: {                                               // OBLIGATORIO
        username: 'su_usuario',
        password: 'su_password'
    }
});
```

## Parametros

### configurar(options)

| Parametro | Tipo | Requerido | Descripcion |
|-----------|------|-----------|-------------|
| credentials.username | string | SI | Usuario proporcionado por Enext |
| credentials.password | string | SI | Contraseña proporcionada por Enext |
| tokenEndpoint | string | NO | URL del endpoint de token |
| biometriaEndpoint | string | NO | URL del endpoint de biometria |

### validar(datos, onSuccess, onError)

| Parametro | Tipo | Descripcion |
|-----------|------|-------------|
| datos.cedula | string | Numero de cedula (10 digitos) |
| datos.codDactilar | string | Codigo dactilar (10 caracteres) |
| onSuccess | function | Callback de exito |
| onError | function | Callback de error |

### Formato del Codigo Dactilar

El codigo dactilar debe tener 10 caracteres con el siguiente formato:
- Posicion 1: Letra (A-Z)
- Posiciones 2-5: Digitos (0-9)
- Posicion 6: Letra (A-Z)
- Posiciones 7-10: Digitos (0-9)

Ejemplo: `V3331V2222`

### Resultado Exitoso

```javascript
{
    accessToken: "eyJhbGciOiJIUzI1NiIs...",
    biometricData: {
        nombre: "JUAN",
        apellido: "PEREZ",
        fechaNacimiento: "1990-01-15",
        // ... mas campos del ciudadano
    },
    timestamp: "2025-12-08T15:30:00.000Z"
}
```

### Codigos de Error

| Codigo | Descripcion |
|--------|-------------|
| CREDENTIALS_NOT_CONFIGURED | No se han configurado las credenciales. Llame a configurar() primero |
| INVALID_PARAMS | Parametros invalidos o faltantes |
| INVALID_CEDULA | Cedula no tiene 10 digitos |
| INVALID_COD_DACTILAR | Codigo dactilar invalido (debe ser 10 caracteres) |
| TOKEN_ERROR | Error al obtener el token |
| TOKEN_PARSE_ERROR | Respuesta de token no valida |
| TOKEN_REQUEST_ERROR | No se pudo conectar al servidor de tokens |
| TOKEN_NETWORK_ERROR | Error de red al obtener token |
| CAMERA_ERROR | Error al acceder a la camara |
| CAMERA_PERMISSION_DENIED | El usuario denego el permiso de camara |
| CANCELLED | El usuario cancelo la validacion |

## Interfaz de Usuario del Plugin

El plugin muestra las siguientes pantallas automaticamente:

1. **Pantalla de carga**: Muestra "Generando token..." mientras obtiene el token
2. **Pantalla de camara**: 
   - Vista previa de la camara dentro de un circulo
   - Boton "Validar" para iniciar la captura
   - Barra de progreso de 3 segundos durante la captura
   - Animacion circular durante la cuenta regresiva
   - Boton "Intentar de Nuevo" si falla la validacion (maximo 3 intentos)
3. **Pantalla de exito**: Muestra los datos del ciudadano y el token
4. **Pantalla de error**: Muestra el mensaje de error con opciones de reintentar o cancelar

## Permisos

El plugin solicita automaticamente los siguientes permisos:

- CAMERA - Para capturar la foto del rostro
- INTERNET - Para comunicacion con los servidores

## Ejemplo Completo de Integracion

```html
<!DOCTYPE html>
<html>
<head>
    <title>Mi App</title>
</head>
<body>
    <h1>Validacion de Identidad</h1>
    
    <input type="text" id="cedula" placeholder="Cedula (10 digitos)">
    <input type="text" id="codDactilar" placeholder="Codigo Dactilar (ej: V3331V2222)">
    <button id="btnValidar">Validar Identidad</button>
    
    <div id="resultado"></div>

    <script src="cordova.js"></script>
    <script>
        // Cuando la app este lista
        document.addEventListener('deviceready', function() {
            
            // 1. Configurar credenciales (OBLIGATORIO, una sola vez)
            EnextBiometria.configurar({
                credentials: {
                    username: 'mi_usuario',
                    password: 'mi_password'
                }
            });
            
            // 2. Configurar el boton de validacion
            document.getElementById('btnValidar').addEventListener('click', function() {
                
                var cedula = document.getElementById('cedula').value;
                var codDactilar = document.getElementById('codDactilar').value;
                
                // 3. Llamar al plugin
                EnextBiometria.validar(
                    {
                        cedula: cedula,
                        codDactilar: codDactilar
                    },
                    function(resultado) {
                        // EXITO
                        document.getElementById('resultado').innerHTML = 
                            '<h2>Validacion Exitosa</h2>' +
                            '<p>Bienvenido: ' + resultado.biometricData.nombre + '</p>' +
                            '<p>Token: ' + resultado.accessToken.substring(0, 50) + '...</p>';
                        
                        // Guardar token para usar en otras peticiones
                        localStorage.setItem('accessToken', resultado.accessToken);
                    },
                    function(error) {
                        // ERROR
                        if (error.code === 'CANCELLED') {
                            // El usuario cancelo, no mostrar error
                            return;
                        }
                        
                        document.getElementById('resultado').innerHTML = 
                            '<h2>Error</h2>' +
                            '<p>' + error.message + '</p>';
                    }
                );
            });
            
        }, false);
    </script>
</body>
</html>
```

## Cancelacion

El usuario puede cancelar la validacion en cualquier momento:
- Presionando el boton de retroceso del dispositivo
- Presionando el boton "Cancelar" en la pantalla de error

El callback `onError` recibira:
```javascript
{
    code: 'CANCELLED',
    message: 'Validacion cancelada'
}
```

## Configuracion por Defecto

El plugin viene configurado con los siguientes endpoints (no es necesario cambiarlos):

- Token: `https://tokens.enext.ltd/token`
- Biometria: `https://biometrico.enext.ltd/validarbiometria`

Las credenciales NO vienen configuradas. Debe solicitarlas a Enext.

## Soporte

Para soporte tecnico o solicitar credenciales:
- Email: soporte@enext.ltd
- Web: https://enext.ltd

## Licencia

MIT License - Copyright (c) 2025 Enext
