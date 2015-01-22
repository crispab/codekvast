//noinspection JSUnusedGlobalSymbols
var codekvastApp = angular.module('codekvastApp', [])

    .config(['$locationProvider', function ($locationProvider) {
        $locationProvider.html5Mode(true);
    }])

    .controller('MainCtrl', ['$scope', '$window', function ($scope, $window) {
        $scope.applications = [];
        $scope.application = undefined;
        $scope.signatures = [];

        $scope.socket = {
            client: null,
            stomp: null
        };

        $scope.updateApplications = function (data) {
            console.log("Received applications %o", data);
            $scope.applications = JSON.parse(data.body);
            if ($scope.applications.length > 0 && angular.isUndefined($scope.application)) {
                $scope.application = $scope.applications[0]
            }
            $scope.$apply()
        };

        $scope.updateSignatures = function (data) {
            console.log("Received signature data=%o", data);
            // TODO: data.body is incremental. Replace existing entries in $scope.signatures.
            $scope.signatures = JSON.parse(data.body);
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
                $scope.socket.stomp.subscribe("/app/applications", $scope.updateApplications);
                $scope.socket.stomp.subscribe("/app/signatures", $scope.updateSignatures);
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
