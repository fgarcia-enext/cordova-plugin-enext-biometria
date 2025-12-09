#!/usr/bin/env node

/**
 * Hook que se ejecuta después de cordova prepare para asegurar que los archivos estén copiados
 */

var fs = require('fs');
var path = require('path');

module.exports = function(context) {
    var platforms = context.opts.platforms || [];
    
    platforms.forEach(function(platform) {
        if (platform === 'android') {
            var projectRoot = context.opts.projectRoot;
            
            // Buscar el archivo fuente en el directorio del plugin instalado
            var pluginWwwDir = path.join(projectRoot, 'plugins', 'cordova-plugin-enext-biometria', 'www');
            var sourceFile = path.join(pluginWwwDir, 'biometria.html');
            
            var targetDir = path.join(projectRoot, 'platforms', 'android', 'app', 'src', 'main', 'assets', 'www', 'plugins', 'enext-biometria');
            var targetFile = path.join(targetDir, 'biometria.html');
            
            // Solo copiar si la plataforma existe
            if (!fs.existsSync(path.join(projectRoot, 'platforms', 'android'))) {
                return;
            }
            
            console.log('Enext Biometria: Verificando archivos de UI...');
            
            // Crear directorio si no existe
            if (!fs.existsSync(targetDir)) {
                fs.mkdirSync(targetDir, { recursive: true });
            }
            
            // Copiar archivo si no existe o es diferente
            if (fs.existsSync(sourceFile)) {
                fs.copyFileSync(sourceFile, targetFile);
                console.log('Enext Biometria: biometria.html actualizado');
            }
        }
    });
};

