process.env.ENV = 'production';

var webpack = require('webpack');
var webpackMerge = require('webpack-merge');
var commonConfig = require('./webpack.common.js');
var helpers = require('./helpers');

module.exports = webpackMerge(commonConfig, {
    mode: 'production',

    devtool: 'source-map',

    output: {
        path: helpers.root('dist'),
        publicPath: '/',
        filename: '[name].[hash].js',
        chunkFilename: '[id].[hash].chunk.js'
    },

    plugins: [
        new webpack.LoaderOptionsPlugin({
            test: /\.html$/,
            options: {
                htmlLoader: {
                    minimize: false // workaround for ng2
                }
            }
        }),
        new webpack.NoEmitOnErrorsPlugin(),

        new webpack.optimize.ModuleConcatenationPlugin()
    ],

    optimization: {
        minimize: true,

        splitChunks: {
            chunks: 'all'
        }
    }
});
