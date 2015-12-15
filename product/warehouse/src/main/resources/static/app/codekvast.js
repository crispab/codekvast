'use strict';
var codekvastWarehouse = angular.module('codekvastWarehouse', ['ngRoute', 'ui.bootstrap'])

    .constant('Defaults', {defaultRoute: 'application-statistics'})

    .config(['$routeProvider', '$httpProvider', '$locationProvider', 'Defaults', function ($routeProvider, $httpProvider, $locationProvider, Defaults) {
        $routeProvider
            .when('/page/:page*', {
                templateUrl: function (routeParams) {
                    return "partials/" + routeParams.page + '.html'
                }
            })

            .otherwise({
                templateUrl: 'partials/' + Defaults.defaultRoute + '.html'
            });

        $httpProvider.defaults.headers.delete = {'Content-Type': 'application/json'};

        $locationProvider.html5Mode(true);

    }])

    .controller('NavigationController', ['$scope', '$location', '$modal', 'Defaults', function ($scope, $location, $modal, Defaults) {
        $scope.menuItems = [
            {
                name: 'Application Usage Statistics',
                url: '/page/application-statistics',
                title: 'Show collection status',
                icon: 'glyphicon-stats'
            },
            {
                name: 'Generate Code Usage Report',
                url: '/page/code-usage-report',
                title: 'Generate reports of code usage',
                icon: 'glyphicon-th-list'
            }
        ];

        $scope.rightMenuItems = [
            {
                name: 'Collector Details',
                url: '/page/collectors',
                title: 'Shows detailed low-level status of the collectors',
                icon: 'glyphicon-dashboard'
            }
        ];

        $scope.isActive = function (viewLocation) {
            return viewLocation === $location.path() || (viewLocation === "/page/" + Defaults.defaultRoute && $location.path() === "/");
        };

    }]);


