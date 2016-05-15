module.exports = function (config) {
    config.set({

        basePath: '',

        frameworks: ['jasmine'],

        files: [
            // paths loaded by Karma
            {pattern: 'node_modules/angular2/bundles/angular2-polyfills.js', included: true, watched: true},
            {pattern: 'node_modules/systemjs/dist/system.src.js', included: true, watched: true},
            {pattern: 'node_modules/rxjs/bundles/Rx.js', included: true, watched: true},
            {pattern: 'node_modules/angular2/bundles/angular2.dev.js', included: true, watched: true},
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

        browsers: ['Firefox'],

        // Karma plugins loaded
        plugins: [
            'karma-jasmine',
            'karma-coverage',
            'karma-chrome-launcher',
            'karma-firefox-launcher',
            'karma-phantomjs-launcher'
        ],

        reporters: ['progress', 'dots', 'coverage'],

        preprocessors: {
            'app/**/!(*spec).js': ['coverage']
        },

        coverageReporter: {
            reporters: [
                {type: 'json', subdir: '.', file: '../../../../build/karma/coverage/coverage-final.json'}
            ]
        }
    })
};
