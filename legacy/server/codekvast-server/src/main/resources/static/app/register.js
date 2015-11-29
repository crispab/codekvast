//noinspection JSUnusedGlobalSymbols
var codekvastRegistration = angular.module('codekvastRegistration', [])

    .controller('RegistrationCtrl', ['$scope', '$http', '$window', function ($scope, $http, $window) {
        $scope.registration = { };

        $scope.alertMessage = undefined;
        $scope.alertClass = undefined;

        $scope.errorMessages = undefined;
        $scope.isSubmitDisabled = false;

        $scope.isFieldValid = function (ctrl) {
            return ctrl.$pending === undefined && ctrl.$valid
        };

        $scope.isFieldInvalid = function (ctrl) {
            return ctrl.$pending === undefined && ctrl.$dirty && ctrl.$invalid
        };

        $scope.isAjaxPending = function (ctrl) {
            return ctrl.$pending;
        };

        $scope.doSubmit = function () {
            if ($scope.form.$valid) {
                $scope.isSubmitDisabled = true;

                $scope.errorMessages = undefined;
                $scope.alertMessage = "Registering...";
                $scope.alertClass = "alert alert-info";

                $http.post("/register", $scope.registration)
                    .success(function () {
                        $http({
                            url: "/login",
                            method: "POST",
                            params: $scope.registration
                        }).success(function () {
                            $window.location.href = "/";
                        }).error(function () {
                            $window.location.href = "/";
                        })
                    }).error(function (data, status, headers, config, statusText) {
                        $scope.alertMessage = statusText || "Registration failed";
                        $scope.alertClass = "alert alert-danger";
                        $scope.isSubmitDisabled = false;
                    });

            } else {
                $scope.errorMessages = [];
                for (var v in $scope.form.$error) {
                    //noinspection JSUnfilteredForInLoop
                    $scope.form.$error[v].forEach(function (field) {
                        var descr = v + " " + field.$name;
                        switch (descr) {
                            case "ckMustMatch emailAddress2":
                                $scope.errorMessages.push("Email addresses must match.");
                                break;
                            case "ckMustMatch pw2":
                                $scope.errorMessages.push("Passwords must match.");
                                break;
                            case "ckUnique username":
                                $scope.errorMessages.push("The username is already taken.");
                                break;
                            case "ckUnique emailAddress":
                                $scope.errorMessages.push("There is another user with that email address.");
                                break;
                            case "ckUnique organisationName":
                                $scope.errorMessages.push("The organisation name is already taken.");
                                break;
                            default:
                                break;
                        }
                    });
                }
                $scope.errorMessages.push("Correct the errors and try again!")
            }
        }
    }])

    .directive('ckMustMatch', [function () {
        return {
            restrict: 'A',
            require: 'ngModel',
            link: function (scope, elem, attrs, ctrl) {
                var firstElement = '#' + attrs.ckMustMatch;
                elem.add(firstElement).on('keyup', function () {
                    scope.$apply(function () {
                        ctrl.$setValidity('ckMustMatch', elem.val() === $(firstElement).val());
                    });
                });
            }
        }
    }])

    .directive('ckMustMatchLc', [function () {
        return {
            restrict: 'A',
            require: 'ngModel',
            link: function (scope, elem, attrs, ctrl) {
                var firstElement = '#' + attrs.ckMustMatchLc;
                elem.add(firstElement).on('keyup', function () {
                    scope.$apply(function () {
                        ctrl.$setValidity('ckMustMatch', angular.lowercase(elem.val()) === angular.lowercase($(firstElement).val()));
                    });
                });
            }
        }
    }])

    .directive('ckLowercase', function () {
        return {
            restrict: 'A',
            require: 'ngModel',
            link: function (scope, element, attrs, ctrl) {
                ctrl.$parsers.push(function (input) {
                    return angular.lowercase(input);
                });
                $(element).css("text-transform", "lowercase");
            }
        };
    })

    .directive('ckUnique', ['$q', '$http', function ($q, $http) {
        return {
            restrict: 'A',
            require: 'ngModel',
            link: function (scope, elm, attrs, ctrl) {

                ctrl.$asyncValidators.ckUnique = function (modelValue, viewValue) {

                    if (ctrl.$isEmpty(modelValue)) {
                        // consider empty model valid
                        return $q.when();
                    }

                    var def = $q.defer();

                    $http.post("/register/isUnique", {kind: attrs.ckUnique, name: viewValue})
                        .success(function (data) {
                            if (data.unique) {
                                def.resolve()
                            } else {
                                def.reject()
                            }
                        }).error(function () {
                            // Assume the value is unique for now. If not, the registration will fail later son the
                            // server side.
                            def.resolve();
                        });

                    return def.promise;
                };
            }
        };
    }])

    .directive('ckPasswordStrength', [function () {
        return {
            replace: false,
            restrict: 'A',
            link: function (scope, elem, attrs) {

                var strength = {
                    colors: ['#F00', '#F90', '#FF0', '#9F0', '#0F0'],

                    measureStrength: function (p) {

                        var result = 0;
                        var symbolsPattern = /[$-/:-?{-~!"^_`\[\]]/g;

                        var hasLowerCaseLetters = /[a-z]+/.test(p);
                        var hasUpperCaseLetters = /[A-Z]+/.test(p);
                        var hasNumbers = /[0-9]+/.test(p);
                        var hasSymbols = symbolsPattern.test(p);

                        var passedMatches = $.grep([hasLowerCaseLetters, hasUpperCaseLetters, hasNumbers, hasSymbols], function (el) {
                            return el === true;
                        }).length;

                        result += 2 * p.length + ((p.length >= 10) ? 1 : 0);
                        result += passedMatches * 10;

                        // penalty (short password)
                        result = (p.length <= 6) ? Math.min(result, 10) : result;

                        // penalty (poor variety of characters)
                        result = (passedMatches == 1) ? Math.min(result, 10) : result;
                        result = (passedMatches == 2) ? Math.min(result, 20) : result;
                        result = (passedMatches == 3) ? Math.min(result, 40) : result;

                        return result;
                    },

                    getColor: function (s) {
                        var idx = 0;
                        if (s <= 10) {
                            idx = 0;
                        } else if (s <= 20) {
                            idx = 1;
                        } else if (s <= 30) {
                            idx = 2;
                        } else if (s <= 40) {
                            idx = 3;
                        } else {
                            idx = 4;
                        }
                        return { idx: idx + 1, col: this.colors[idx] };
                    }
                };

                scope.$watch(attrs.ckPasswordStrength, function (newValue) {
                    if (newValue === undefined) {
                        elem.css({ "display": "none"  });
                    } else {
                        var c = strength.getColor(strength.measureStrength(newValue));
                        elem.css({ "display": "inline" });
                        elem.children('li')
                            .css({ "background": "#DDD" })
                            .slice(0, c.idx)
                            .css({ "background": c.col });
                    }
                });

            },
            template: '<li class="point"></li><li class="point"></li><li class="point"></li><li class="point"></li><li class="point"></li>'
        }
    }]);
