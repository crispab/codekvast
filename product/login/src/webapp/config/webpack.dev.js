process.env.CODEKVAST_VERSION = 'dev';
process.env.ENV = 'development';

var webpackMerge = require('webpack-merge');
var ExtractTextPlugin = require('extract-text-webpack-plugin');
var commonConfig = require('./webpack.common.js');
var helpers = require('./helpers');

module.exports = webpackMerge(commonConfig, {
    devtool: 'cheap-module-eval-source-map',

    output: {
        path: helpers.root('dist'),
        publicPath: 'http://localhost:8088/',
        filename: '[name].js',
        chunkFilename: '[id].chunk.js'
    },

    plugins: [
        new ExtractTextPlugin('[name].css')
    ],

    devServer: {
        inline: true,
        port: 8088,
        historyApiFallback: true,
        stats: 'minimal',
        proxy: {
            '/api': 'http://localhost:8080',
            '/oauth': 'http://localhost:8080'
        }
    }
});
