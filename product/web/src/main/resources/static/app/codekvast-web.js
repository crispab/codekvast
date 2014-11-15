var codekvastWeb = angular.module('codekvastWeb', ['ngRoute'])
    .controller('MainController', ['$scope', '$location', function ($scope, $location) {
        $scope.location = $location;
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

        $locationProvider.html5Mode(true);
    }])

    .run(['$rootScope', '$location', '$log', function ($rootScope, $location, $log) {
        $rootScope.$on('$viewContentLoaded', function () {
            $log.info("Viewing " + $location.path())
            ga('send', 'pageview', $location.path());
        });
    }]);

