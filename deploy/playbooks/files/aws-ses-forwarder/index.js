var LambdaForwarder = require("aws-lambda-ses-forwarder");

exports.handler = function(event, context, callback) {
  // Configure the S3 bucket and key prefix for stored raw emails, and the
  // mapping of email addresses to forward from and to.
  //
  // Expected keys/values:
  // - fromEmail: Forwarded emails will come from this verified address
  // - emailBucket: S3 bucket name where SES stores emails.
  // - emailKeyPrefix: S3 key name prefix where SES stores email. Include the
  //   trailing slash.
  // - forwardMapping: Object where the key is the email address from which to
  //   forward and the value is an array of email addresses to which to send the
  //   message.
  var overrides = {
    config: {
      subjectPrefix: "",
      emailBucket: "mailbox.codekvast.io",
      emailKeyPrefix: "inbox/",
      forwardMapping: {
        "support@codekvast.io": [
          "codekvast-support@hit.se" // NOTE: This email address must be validated in the SES console!
        ],
        "no-reply@codekvast.io": [
          "codekvast-no-reply@hit.se" // NOTE: This email address must be validated in the SES console!
        ]
      }
    }
  };
  LambdaForwarder.handler(event, context, callback, overrides);
};
