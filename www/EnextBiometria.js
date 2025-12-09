/**
 * Enext Biometria Plugin
 * Plugin de validacion biometrica facial para Cordova
 * 
 * @author Enext
 * @version 1.0.0
 */

var exec = require('cordova/exec');

var EnextBiometria = {
    
    /**
     * Configuracion del plugin.
     * El cliente DEBE configurar las credenciales antes de usar validar()
     */
    config: {
        tokenEndpoint: 'https://tokens.enext.ltd/token',
        biometriaEndpoint: 'https://biometrico.enext.ltd/validarbiometria',
        credentials: null  // El cliente debe configurar sus credenciales
    },

    /**
     * Configura los endpoints y credenciales del plugin.
     * OBLIGATORIO: Llamar antes de validar() para configurar las credenciales.
     * 
     * @param {Object} options - Opciones de configuracion
     * @param {string} options.tokenEndpoint - URL del endpoint de token (opcional)
     * @param {string} options.biometriaEndpoint - URL del endpoint de biometria (opcional)
     * @param {Object} options.credentials - Credenciales (OBLIGATORIO)
     * @param {string} options.credentials.username - Nombre de usuario
     * @param {string} options.credentials.password - Contraseña
     * 
     * @example
     * EnextBiometria.configurar({
     *     credentials: {
     *         username: 'mi_usuario',
     *         password: 'mi_password'
     *     }
     * });
     */
    configurar: function(options) {
        if (options.tokenEndpoint) {
            this.config.tokenEndpoint = options.tokenEndpoint;
        }
        if (options.biometriaEndpoint) {
            this.config.biometriaEndpoint = options.biometriaEndpoint;
        }
        if (options.credentials) {
            this.config.credentials = options.credentials;
        }
    },

    /**
     * Inicia el proceso de validacion biometrica.
     * IMPORTANTE: Debe llamar a configurar() primero para establecer las credenciales.
     * 
     * @param {Object} datos - Datos del ciudadano
     * @param {string} datos.cedula - Numero de cedula (10 digitos)
     * @param {string} datos.codDactilar - Codigo dactilar (10 caracteres, ej: V3331V2222)
     * @param {Function} onSuccess - Callback de exito con {accessToken, biometricData, timestamp}
     * @param {Function} onError - Callback de error con {code, message}
     * 
     * @example
     * // Primero configurar credenciales
     * EnextBiometria.configurar({
     *     credentials: { username: 'mi_usuario', password: 'mi_password' }
     * });
     * 
     * // Luego validar
     * EnextBiometria.validar(
     *     { cedula: '1234567890', codDactilar: 'V3331V2222' },
     *     function(resultado) {
     *         console.log('Token:', resultado.accessToken);
     *     },
     *     function(error) {
     *         console.error('Error:', error.message);
     *     }
     * );
     */
    validar: function(datos, onSuccess, onError) {
        // Validar que las credenciales esten configuradas
        if (!this.config.credentials || !this.config.credentials.username || !this.config.credentials.password) {
            if (onError) {
                onError({
                    code: 'CREDENTIALS_NOT_CONFIGURED',
                    message: 'Debe configurar las credenciales antes de validar. Use EnextBiometria.configurar()'
                });
            }
            return;
        }

        // Validar que se proporcionen los datos requeridos
        if (!datos || !datos.cedula || !datos.codDactilar) {
            if (onError) {
                onError({
                    code: 'INVALID_PARAMS',
                    message: 'Se requiere cedula y codigo dactilar'
                });
            }
            return;
        }

        // Validar formato de cedula (10 digitos)
        if (!/^\d{10}$/.test(datos.cedula)) {
            if (onError) {
                onError({
                    code: 'INVALID_CEDULA',
                    message: 'La cedula debe tener 10 digitos'
                });
            }
            return;
        }

        // Validar codigo dactilar (10 caracteres: letra + 4 digitos + letra + 4 digitos)
        if (!/^[A-Z]\d{4}[A-Z]\d{4}$/.test(datos.codDactilar.toUpperCase())) {
            if (onError) {
                onError({
                    code: 'INVALID_COD_DACTILAR',
                    message: 'El codigo dactilar debe tener 10 caracteres (ej: V3331V2222)'
                });
            }
            return;
        }

        // Preparar argumentos para el plugin nativo
        var args = [
            datos.cedula,
            datos.codDactilar.toUpperCase(),
            this.config.tokenEndpoint,
            this.config.biometriaEndpoint,
            this.config.credentials.username,
            this.config.credentials.password
        ];

        // Ejecutar plugin nativo
        exec(
            function(result) {
                // Exito - formatear resultado
                if (onSuccess) {
                    onSuccess({
                        accessToken: result.accessToken,
                        biometricData: result.biometricData || null,
                        timestamp: result.timestamp || new Date().toISOString()
                    });
                }
            },
            function(error) {
                // Error - formatear mensaje
                if (onError) {
                    onError({
                        code: error.code || 'UNKNOWN_ERROR',
                        message: error.message || 'Error desconocido en la validacion'
                    });
                }
            },
            'EnextBiometria',
            'validar',
            args
        );
    },

    /**
     * Cancela el proceso de validacion en curso.
     * 
     * @param {Function} onSuccess - Callback de exito
     * @param {Function} onError - Callback de error
     */
    cancelar: function(onSuccess, onError) {
        exec(onSuccess, onError, 'EnextBiometria', 'cancelar', []);
    },

    /**
     * Obtiene la version del plugin.
     * 
     * @param {Function} callback - Callback con la version
     */
    getVersion: function(callback) {
        if (callback) {
            callback('1.0.0');
        }
    }
};

module.exports = EnextBiometria;
