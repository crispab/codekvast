process.env.CHROME_BIN = require('puppeteer').executablePath()
module.exports = require('./config/karma.conf.js');

