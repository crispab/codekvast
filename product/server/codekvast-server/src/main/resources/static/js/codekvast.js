var codekvastApp = angular.module('codekvastApp', [])
    .controller('SignaturesCtrl', ['$scope', function ($scope) {
        $scope.signatures = [];

        $scope.socket = {
            client: null,
            stomp: null
        };

        $scope.updateSignatures = function (data) {
            console.log("Received signature data=%o", data)
            // TODO: data.body is incremental. Replace existing entries in $scope.signatures.
            $scope.signatures = JSON.parse(data.body)
            $scope.$apply()
        };

        $scope.reconnect = function () {
            setTimeout($scope.initSockets, 10000);
        };

        $scope.initSockets = function () {
            $scope.socket.client = new SockJS("/websocket", null, {debug: true});

            $scope.socket.stomp = Stomp.over($scope.socket.client);

            $scope.socket.stomp.connect({
                login: 'user',
                passcode: '0000',
                'client-id': 'my-client-id'
            }, function () {
                $scope.socket.stomp.subscribe("/topic/signatures", $scope.updateSignatures);
                $scope.socket.stomp.send("/app/hello", {}, "Hello!")
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
