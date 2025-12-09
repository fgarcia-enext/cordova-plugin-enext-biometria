#!/usr/bin/env node

/**
 * Hook que copia los archivos HTML del plugin a la carpeta de assets de Android
 */

var fs = require('fs');
var path = require('path');

module.exports = function(context) {
    var platforms = context.opts.platforms || [];
    
    if (platforms.indexOf('android') === -1) {
        return;
    }
    
    var pluginDir = context.opts.plugin.dir;
    var projectRoot = context.opts.projectRoot;
    
    var sourceFile = path.join(pluginDir, 'www', 'biometria.html');
    var targetDir = path.join(projectRoot, 'platforms', 'android', 'app', 'src', 'main', 'assets', 'www', 'plugins', 'enext-biometria');
    var targetFile = path.join(targetDir, 'biometria.html');
    
    console.log('Enext Biometria: Copiando archivos de UI...');
    
    // Crear directorio si no existe
    if (!fs.existsSync(targetDir)) {
        fs.mkdirSync(targetDir, { recursive: true });
    }
    
    // Copiar archivo
    if (fs.existsSync(sourceFile)) {
        fs.copyFileSync(sourceFile, targetFile);
        console.log('Enext Biometria: biometria.html copiado exitosamente');
    } else {
        console.error('Enext Biometria: No se encontró el archivo fuente:', sourceFile);
    }
};

