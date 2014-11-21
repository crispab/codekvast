var codekvastWeb = angular.module('codekvastWeb', ['ngRoute', 'ngAria', 'ui.bootstrap'])
    .controller('MainController', ['$scope', '$location', '$templateCache', '$http', function ($scope, $location, $templateCache, $http) {
        $scope.data = {emailAddress: null};
        $scope.isSubmitDisabled = false;
        $scope.errorMessage = undefined;

        $scope.cleanTemplateCache = function () {
            $templateCache.removeAll();
        };

        $scope.submitEmailAddress = function () {
            if ($scope.data.emailAddress) {
                $scope.isSubmitDisabled = true;

                $http.post("/register", $scope.data)
                    .success(function () {
                        $location.path("/page/thank-you");
                    }).error(function (data, status, headers, config, statusText) {
                        $scope.errorMessage = statusText || "RegistrationRequest failed";
                        $location.path("/page/oops");
                        $scope.isSubmitDisabled = false;
                    });
            }
        };

        $scope.hasErrorMessage = function () {
            if ($scope.errorMessage === undefined) {
                $location.path('/');
                return false;
            }
            return true;
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

