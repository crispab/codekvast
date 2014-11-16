var codekvastWeb = angular.module('codekvastWeb', ['ngRoute', 'ui.bootstrap'])
    .controller('MainController', ['$scope', '$location', '$templateCache', function ($scope, $location, $templateCache) {
        $scope.location = $location;

        $scope.cleanTemplateCache = function () {
            $templateCache.removeAll();
        }
    }])

    .config(['$routeProvider', '$locationProvider', function ($routeProvider, $locationProvider) {
        $routeProvider
            .when('/page1', {
                templateUrl: 'page1.html'
            })

            .when('/page2', {
                templateUrl: 'page2.html'
            })

            .otherwise({
                templateUrl: 'welcome.html'
            })
        ;

        // $locationProvider.html5Mode(true);
    }])

    .run(['$rootScope', '$location', '$log', function ($rootScope, $location, $log) {
        $rootScope.$on('$viewContentLoaded', function () {
            $log.info("Viewing " + $location.path())
            ga('send', 'pageview', $location.path());
        });
    }]);

