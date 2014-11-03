//noinspection JSUnusedGlobalSymbols
var codekvastRegistration = angular.module('codekvastRegistration', [])
    .controller('RegistrationCtrl', ['$scope', function ($scope) {
        $scope.registration = { };

        $scope.form = null;

        $scope.doSubmit = function () {
            if ($scope.form && $scope.form.$valid) {
                alert("Submitting " + JSON.stringify($scope.registration))
            }
        }
    }])

    .directive('mustMatch', [function () {
        return {
            require: 'ngModel',
            link: function (scope, elem, attrs, ctrl) {
                var firstElement = '#' + attrs.mustMatch;
                elem.add(firstElement).on('keyup', function () {
                    scope.$apply(function () {
                        ctrl.$setValidity('mustmatch', elem.val() === $(firstElement).val());
                    });
                });
            }
        }
    }])

    .directive('checkStrength', [function () {
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

                scope.$watch(attrs.checkStrength, function (newValue) {
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
