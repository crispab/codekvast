var webpack = require('webpack');
var CopyWebpackPlugin = require('copy-webpack-plugin');
var HtmlWebpackPlugin = require('html-webpack-plugin');
var ExtractTextPlugin = require('extract-text-webpack-plugin');
var helpers = require('./helpers');

module.exports = {
    entry: {
        'polyfills': './src/polyfills.ts',
        'vendor': './src/vendor.ts',
        'app': './src/main.ts'
    },

    bail: true,

    resolve: {
        extensions: ['.js', '.ts']
    },

    module: {
        rules: [
            {
                test: /\.ts$/,
                loaders: ['awesome-typescript-loader', 'angular2-template-loader']
            },
            {
                test: /\.html$/,
                loader: 'html-loader'
            },
            {
                test: /\.(png|jpe?g|gif|svg|woff|woff2|ttf|eot|ico)$/,
                loader: 'file-loader?name=assets/[name].[hash].[ext]'
            },
            {
                test: /\.css$/,
                exclude: helpers.root('src', 'app'),
                loader: ExtractTextPlugin.extract({fallback: 'style-loader', use: 'css-loader?sourceMap'})
            },
            {
                test: /\.css$/,
                include: helpers.root('src', 'app'),
                loader: 'raw-loader'
            }
        ]
    },

    plugins: [
        new webpack.EnvironmentPlugin([
            'CODEKVAST_VERSION'
        ]),

        new webpack.optimize.CommonsChunkPlugin({
            name: ['app', 'vendor', 'polyfills']
        }),

        new CopyWebpackPlugin([
            { from: 'static' }
        ]),

        new HtmlWebpackPlugin({
            inject: false,
            template: require('html-webpack-template'),
            title: 'Codekvast',
            links: [
                'https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0-alpha.6/css/bootstrap.min.css',
                'https://maxcdn.bootstrapcdn.com/font-awesome/4.7.0/css/font-awesome.min.css'
            ],
            scripts: [
                'https://s3.amazonaws.com/assets.heroku.com/boomerang/boomerang.js'
            ],
            devServer: 'http://localhost:8088',
            googleAnalytics: {
                trackingId: 'UA-97240168-1',
                pageViewOnLoad: true
            },
            mobile: true,
            minify: false,
            appMountId: 'app',
            window: {
                CODEKVAST_API: process.env.CODEKVAST_API,
                CODEKVAST_VERSION: process.env.CODEKVAST_VERSION || 'dev'
            }
        })

        // TODO: add favicons-webpack-plugin
    ]
};

