var webpack = require('webpack');
var CopyWebpackPlugin = require('copy-webpack-plugin');
var HtmlWebpackPlugin = require('html-webpack-plugin');
var MiniCssExtractPlugin = require("mini-css-extract-plugin");
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
                use: [{loader: MiniCssExtractPlugin.loader,}, "css-loader"]
            },
            {
                test: /\.css$/,
                include: helpers.root('src', 'app'),
                loader: 'raw-loader'
            }

        ]
    },

    plugins: [
        new webpack.EnvironmentPlugin({
            CODEKVAST_VERSION: 'dev',
            ENV: 'development'
        }),

        new CopyWebpackPlugin([
            {from: 'static'}
        ]),

        new MiniCssExtractPlugin({
            filename: "[name].css",
            chunkFilename: "[id].css"
        }),

        new HtmlWebpackPlugin({
            inject: false,
            template: require('html-webpack-template'),
            title: 'Codekvast',
            links: [
                {
                    rel: 'stylesheet',
                    href: 'https://use.fontawesome.com/releases/v5.0.9/css/all.css',
                    integrity: 'sha384-5SOiIsAziJl6AWe0HWRKTXlfcSHKmYV4RBF18PPJ173Kzn7jzMyFuTtk8JA7QQG1',
                    crossorigin: 'anonymous'
                },
                {
                    rel: 'stylesheet',
                    href: 'https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/css/bootstrap.min.css',
                    integrity: 'sha384-Gn5384xqQ1aoWXA+058RXPxPg6fy4IWvTNh0E263XmFcJlSAwiGgFAW/dAiS6JXm',
                    crossorigin: 'anonymous'
                }
            ],
            scripts: [
                'https://s3.amazonaws.com/assets.heroku.com/boomerang/boomerang.js',
                'https://www.google-analytics.com/analytics.js'
            ],
            // Google Analytics is initialized in app.component.ts
            mobile: true,
            minify: false,
            appMountId: 'app',
            headHtmlSnippet: '<style>div.app-loading {position: fixed;top: 30%;left: 20%; font-size: xx-large}</style>',
            appMountHtmlSnippet: '<div class="app-loading"><h1>Loading Codekvast Dashboard...</h1></div>',
            window: {
                CODEKVAST_VERSION: process.env.CODEKVAST_VERSION || 'dev'
            }
        })
    ]
};

