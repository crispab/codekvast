var codekvastRegistration = angular.module('codekvastRegistration', [])
    .controller('RegistrationCtrl', ['$scope', function ($scope) {
        $scope.registration = {
            username: null,
            password: null
        }

        $scope.password2 = null,

            $scope.isFormInvalid = true

        $scope.validateForm = function () {
            $scope.isFormInvalid = $scope.registration.username === null
        }

    }])
    .directive('pwCheck', [function () {
        return {
            require: 'ngModel',
            link: function (scope, elem, attrs, ctrl) {
                var firstPassword = '#' + attrs.pwCheck;
                elem.add(firstPassword).on('keyup', function () {
                    scope.$apply(function () {
                        ctrl.$setValidity('pwmatch', elem.val() === $(firstPassword).val());
                    });
                });
            }
        }
    }]);
