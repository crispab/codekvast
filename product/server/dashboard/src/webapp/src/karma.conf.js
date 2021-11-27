// Karma configuration file, see link for more information
// https://karma-runner.github.io/1.0/config/configuration-file.html

module.exports = function (config) {
  config.set({
      basePath: '',
      frameworks: ['jasmine', '@angular-devkit/build-angular'],
      plugins: [
          require('karma-chrome-launcher'),
          require('karma-coverage'),
          require('karma-firefox-launcher'),
          require('karma-jasmine'),
          require('karma-jasmine-html-reporter'),
          require('karma-junit-reporter'),
          require('@angular-devkit/build-angular/plugins/karma')
      ],
      client: {
          clearContext: false // leave Jasmine Spec Runner output visible in browser
      },
      coverageReporter: {
          dir: require('path').join(__dirname, '../../../build/reports/frontend-coverage'),
      },
      junitReporter: {
          outputDir: require('path').join(__dirname, '../../../build/test-results/frontendTest'),
          useBrowserName: true, // add browser name to report and classes names
      },
      reporters: ['progress', 'junit', 'kjhtml'],
      port: 9876,
      colors: true,
      logLevel: config.LOG_INFO,
      autoWatch: true,
      browsers: ['Chrome', 'Firefox'],
      customLaunchers: {
          ChromeHeadlessCI: {
              base: 'ChromeHeadless',
              flags: ['--no-sandbox']
          }
      }, singleRun: false,
      restartOnFileChange: true
  });
};
