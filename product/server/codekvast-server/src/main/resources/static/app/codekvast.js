//noinspection JSUnusedGlobalSymbols
var codekvastApp = angular.module('codekvastApp', [])

    .config(['$locationProvider', function ($locationProvider) {
        $locationProvider.html5Mode(true);
    }])

    .controller('MainCtrl', ['$scope', '$window', function ($scope, $window) {
        $scope.customerNames = [];
        $scope.applications = [];
        $scope.versions = [];
        $scope.packages = [];

        $scope.customerName = undefined;
        $scope.application = undefined;
        $scope.version = undefined;
        $scope.package = undefined;

        $scope.socket = {
            client: null,
            stomp: null
        };

        $scope.updateFilterValues = function (data) {
            console.log("Received filter values %o", data);
            var body = JSON.parse(data.body);
            $scope.customerNames = body.customerNames;
            $scope.applications = body.applications;
            $scope.versions = body.versions;
            $scope.packages = body.packages;
            $scope.$apply()

        };

        $scope.loggedOut = function () {
            console.log("Logged out");
            // Cannot use $location here, since /login is outside the Angular app
            $window.location.href = "/login?logout";
        };

        $scope.initSockets = function () {
            $scope.socket.client = new SockJS("/codekvast", null, {debug: true});

            $scope.socket.stomp = Stomp.over($scope.socket.client);

            $scope.socket.stomp.connect({}, function () {
                $scope.socket.stomp.subscribe("/app/filterValues", $scope.updateFilterValues);
                $scope.socket.stomp.subscribe("/user/queue/filterValues", $scope.updateFilterValues);
            }, function (error) {
                console.log("Cannot connect %o", error)
            });
            $scope.socket.client.onclose = $scope.loggedOut;
        };

        $scope.initSockets();
    }])

    .filter('suppressEmptyDate', function () {
        return function (input) {
            return input == 0 ? "" : input;
        };
    });
