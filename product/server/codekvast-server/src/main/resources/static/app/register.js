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
    }]);
