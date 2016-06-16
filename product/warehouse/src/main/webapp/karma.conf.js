module.exports = function (config) {
    config.set({

        basePath: '',

        browsers: ['PhantomJS'],

        plugins: [
            'karma-jasmine',
            'karma-coverage',
            'karma-chrome-launcher',
            'karma-firefox-launcher',
            'karma-phantomjs-launcher'
        ],

        frameworks: ['jasmine'],

        files: [
            'node_modules/es6-shim/es6-shim.min.js',
            'node_modules/core-js/client/shim.min.js',
            'node_modules/zone.js/dist/zone.js',
            'node_modules/reflect-metadata/Reflect.js',
            'node_modules/systemjs/dist/system.src.js',
            'node_modules/intl/dist/Intl.min.js',
            'node_modules/intl/locale-data/jsonp/en.js',
            'karma-test-shim.js',
            'systemjs.config.js',

            // Our built application code
            {pattern: 'app/**/*.js', included: false, watched: true},
            {pattern: 'app/**/*.html', included: false, watched: true},
            {pattern: 'app/**/*.css', included: false, watched: true},

            // paths to support debugging with source maps in dev tools
            {pattern: 'app/**/*.ts', included: false, watched: true},
            {pattern: 'app/**/*.js.map', included: false, watched: false}
        ],

        // proxied base paths
        proxies: {
            // required for component assets fetched by Angular's compiler
            '/app/': '/base/app/'
        },

        port: 9876,

        logLevel: config.LOG_INFO,

        colors: true,

        reporters: ['progress', 'dots', 'coverage'],

        preprocessors: {
            'app/**/!(*spec).js': ['coverage']
        },

        coverageReporter: {
            reporters: [
                {type: 'json', subdir: '../../../../build/karma', file: 'coverage-final.json'}
            ]
        }
    })
};
