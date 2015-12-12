'use strict';
var codekvastWarehouse = angular.module('codekvastWarehouse', [])

    .config(['$locationProvider', function ($locationProvider) {
        $locationProvider.html5Mode(true);
    }]);
