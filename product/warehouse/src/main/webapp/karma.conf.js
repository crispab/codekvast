module.exports = function (config) {
    config.set({

        basePath: '',

        browsers: ['PhantomJS'],

        plugins: [
            'karma-jasmine',
            'karma-coverage',
            'karma-chrome-launcher',
            'karma-firefox-launcher',
            'karma-phantomjs-launcher',
            'karma-phantomjs-shim'
        ],

        frameworks: ['jasmine', 'phantomjs-shim'],

        files: [
            // paths loaded by Karma
            {pattern: 'node_modules/es6-shim/es6-shim.min.js', included: true, watched: true},
            {pattern: 'node_modules/intl/dist/Intl.min.js', included: true, watched: true},
            {pattern: 'node_modules/intl/locale-data/jsonp/en.js', included: true, watched: true},
            {pattern: 'node_modules/angular2/bundles/angular2-polyfills.js', included: true, watched: true},
            {pattern: 'node_modules/systemjs/dist/system.src.js', included: true, watched: true},
            {pattern: 'node_modules/rxjs/bundles/Rx.js', included: true, watched: true},
            {pattern: 'node_modules/angular2/bundles/angular2.dev.js', included: true, watched: true},
            {pattern: 'node_modules/angular2/bundles/http.dev.js', included: true, watched: true},
            {pattern: 'node_modules/angular2/bundles/testing.dev.js', included: true, watched: true},
            {pattern: 'karma-test-shim.js', included: true, watched: true},

            // paths loaded via module imports
            {pattern: 'app/**/*.js', included: false, watched: true},

            // paths to support debugging with source maps in dev tools
            {pattern: 'app/**/*.ts', included: false, watched: false},
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
