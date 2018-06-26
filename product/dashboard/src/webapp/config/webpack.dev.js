process.env.CODEKVAST_VERSION = 'dev';
process.env.ENV = 'development';

var webpackMerge = require('webpack-merge');
var commonConfig = require('./webpack.common.js');
var helpers = require('./helpers');

module.exports = webpackMerge(commonConfig, {
    mode: 'development',

    devtool: 'cheap-module-eval-source-map',

    output: {
        path: helpers.root('dist'),
        publicPath: '/',
        filename: '[name].js',
        chunkFilename: '[id].chunk.js'
    },

    devServer: {
        inline: true,
        port: 8089,
        historyApiFallback: {
            disableDotRule: true
        },
        stats: 'minimal',
        headers: {
            'Access-Control-Allow-Origin': 'http://localhost:8080',
            'Access-Control-Allow-Headers': 'Content-Type',
            'Access-Control-Allow-Credentials': true
        },
        proxy: {
            '/api-docs': 'http://localhost:8081',
            '/dashboard': 'http://localhost:8081',
            '/swagger': 'http://localhost:8081',
            '/webjars': 'http://localhost:8081'
        }
    }
});
