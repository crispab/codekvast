var webpackConfig = require('./webpack.test');

module.exports = function (config) {
    var _config = {
        autoWatch: false,
        basePath: '',
        browsers: process.env.DISPLAY ? ['PhantomJS', 'Chrome', 'Firefox'] : ['PhantomJS'],
        colors: true,
        files: [{pattern: './config/karma-test-shim.js', watched: false}],
        frameworks: ['jasmine'],
        logLevel: config.LOG_INFO,
        port: 9876,
        preprocessors: { './config/karma-test-shim.js': ['webpack', 'sourcemap']},
        reporters: ['mocha'],
        singleRun: true,
        webpack: webpackConfig,
        webpackMiddleware: { stats: 'errors-only'},
        webpackServer: { noInfo: true}
    };

    config.set(_config);
};

