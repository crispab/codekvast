//noinspection JSUnusedGlobalSymbols
var codekvastApp = angular.module('codekvastApp', [])
    .controller('SignaturesCtrl', ['$scope', function ($scope) {
        $scope.applications = [];
        $scope.signatures = [];

        $scope.socket = {
            client: null,
            stomp: null
        };

        $scope.updateApplications = function (data) {
            console.log("Received applications %o", data);
            $scope.applications = JSON.parse(data.body);
            $scope.$apply()
        };

        $scope.updateSignatures = function (data) {
            console.log("Received signature data=%o", data);
            // TODO: data.body is incremental. Replace existing entries in $scope.signatures.
            $scope.signatures = JSON.parse(data.body);
            $scope.$apply()
        };

        $scope.reconnect = function () {
            setTimeout($scope.initSockets, 10000);
        };

        $scope.initSockets = function () {
            $scope.socket.client = new SockJS("/codekvast", null, {debug: true});

            $scope.socket.stomp = Stomp.over($scope.socket.client);

            $scope.socket.stomp.connect({}, function (frame) {
                $scope.socket.stomp.subscribe("/app/applications", $scope.updateApplications);
                // $scope.socket.stomp.subscribe("/app/signatures", $scope.updateSignatures);
            }, function (error) {
                console.log("Cannot connect %o", error)
            });
            $scope.socket.client.onclose = $scope.reconnect;
        };

        $scope.initSockets();
    }])
    .filter('suppressEmptyDate', function () {
        return function (input) {
            return input == 0 ? "" : input;
        };
    });
