var codekvastWeb = angular.module('codekvastWeb', ['ngRoute', 'ngAria', 'ui.bootstrap'])
    .controller('MainController', ['$scope', '$location', '$templateCache', function ($scope, $location, $templateCache) {
        $scope.location = $location;

        $scope.cleanTemplateCache = function () {
            $templateCache.removeAll();
        }
    }])

    .config(['$routeProvider', '$locationProvider', function ($routeProvider, $locationProvider) {
        $routeProvider
            .when('/page/:page*', {
                templateUrl: function (routeParams) {
                    return "p/" + routeParams.page + '.html'
                }
            })

            .otherwise({
                templateUrl: 'p/welcome.html'
            });

        $locationProvider.html5Mode(true);
    }])

    .run(['$rootScope', '$location', '$log', '$document', function ($rootScope, $location, $log, $document) {
        $rootScope.$on('$viewContentLoaded', function () {
            var title = angular.element('.window-title').text();
            $log.info("Viewing '" + $location.path() + "' ('" + title + "')");
            $document[0].title = title;
            ga('send', 'pageview', $location.path());
        });
    }]);

