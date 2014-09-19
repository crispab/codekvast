var codekvastControllers = angular.module('codekvastControllers', ['codekvastServices']);

codekvastControllers.controller('SignaturesCtrl', ['$scope', '$http',
    function ($scope, $http) {
        $http.get('/user/signatures/').success(function (data) {
            $scope.signatures = data;
        });

        $scope.orderProp = 'usedAtMillis';
    }]);

