//noinspection JSUnusedGlobalSymbols
var codekvastApp = angular.module('codekvastApp', [])

    .config(['$locationProvider', function ($locationProvider) {
        $locationProvider.html5Mode(true);
    }])

    .controller('MainCtrl', ['$scope', '$window', function ($scope, $window) {
        $scope.filterValues = {
            applications: [],
            versions: [],
            packages: []
        };

        $scope.application = undefined;
        $scope.version = undefined;
        $scope.package = undefined;

        $scope.signatures = [];

        $scope.socket = {
            client: null,
            stomp: null
        };

        $scope.updateFilterValues = function (data) {
            console.log("Received filter values %o", data);
            $scope.$apply(function () {
                $scope.filterValues = JSON.parse(data.body);
            });
        };

        $scope.updateSignatures = function(data) {
            console.log("Received signatures");
            $scope.$apply(function() {
                $scope.signatures = JSON.parse(data.body);
            })
        }

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
                $scope.socket.stomp.subscribe("/app/signatures", $scope.updateSignatures);
                $scope.socket.stomp.subscribe("/user/queue/signatures", $scope.updateSignatures);
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
