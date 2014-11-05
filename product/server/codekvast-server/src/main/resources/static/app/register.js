//noinspection JSUnusedGlobalSymbols
var codekvastRegistration = angular.module('codekvastRegistration', [])

    .controller('RegistrationCtrl', ['$scope', function ($scope) {
        $scope.registration = { };

        $scope.form = null;

        $scope.errorMessages = undefined;

        $scope.isFieldValid = function (ctrl) {
            return ctrl.$pending === undefined && ctrl.$valid
        };

        $scope.isFieldInvalid = function (ctrl) {
            return ctrl.$pending === undefined && ctrl.$invalid && !ctrl.$error.unique
        };

        $scope.doSubmit = function () {
            if ($scope.form && $scope.form.$valid) {
                $scope.errorMessages = undefined;
                alert("Submitting " + JSON.stringify($scope.registration))
            } else {
                $scope.errorMessages = [];

                if ($scope.emailAddress2 === undefined) {
                    $scope.errorMessages.push("Email addresses do not match!")
                }
                if ($scope.pw2 === undefined) {
                    $scope.errorMessages.push("Passwords do not match!")
                }
                $scope.errorMessages.push("Correct the errors and try again!")
            }
        }
    }])

    .directive('ckMustMatch', [function () {
        return {
            require: 'ngModel',
            link: function (scope, elem, attrs, ctrl) {
                var firstElement = '#' + attrs.ckMustMatch;
                elem.add(firstElement).on('keyup', function () {
                    scope.$apply(function () {
                        ctrl.$setValidity('ckmustmatch', elem.val() === $(firstElement).val());
                    });
                });
            }
        }
    }])

    .directive('ckLowercase', function () {
        return {
            require: 'ngModel',
            link: function (scope, element, attrs, ctrl) {
                ctrl.$parsers.push(function (input) {
                    return input ? input.toLowerCase() : "";
                });
                $(element).css("text-transform", "lowercase");
            }
        };
    })

    .directive('unique', ['$q', '$timeout', '$http', function ($q, $timeout, $http) {
        return {
            require: 'ngModel',
            link: function (scope, elm, attrs, ctrl) {

                ctrl.$asyncValidators.unique = function (modelValue, viewValue) {

                    if (ctrl.$isEmpty(modelValue)) {
                        // consider empty model valid
                        return $q.when();
                    }

                    var def = $q.defer();

                    $http.get("/register/isUnique",
                        {params: {
                            kind: attrs.unique,
                            value: viewValue.toLowerCase()
                        }
                        })
                        .success(function (data) {
                            if (data) {
                                def.resolve()
                            } else {
                                def.reject()
                            }
                        }).error(function () {
                            // Assume the value is unique for now. If not, the registration will fail later son the
                            // server side.
                            def.resolve();
                        })

                    return def.promise;
                };
            }
        };
    }])

    .directive('ckPasswordStrength', [function () {
        return {
            replace: false,
            restrict: 'EACM',
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
