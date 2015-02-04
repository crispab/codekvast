//noinspection JSUnusedGlobalSymbols
var codekvastApp = angular.module('codekvastApp', ['ui.bootstrap'])

    .config(['$locationProvider', function ($locationProvider) {
        $locationProvider.html5Mode(true);
    }])

    .controller('MainCtrl', ['$scope', '$window', function ($scope, $window) {
        $scope.connected = false;
        $scope.haveData = false;
        $scope.maxRows = 100;
        $scope.progress = undefined;
        $scope.progressMax = undefined;

        $scope.application = undefined;
        $scope.version = undefined;
        $scope.signatures = [];
        $scope.timestamp = undefined;

        $scope.sortField = 'invokedAtMillis';
        $scope.reverse = false;

        $scope.socket = {
            client: null,
            stomp: null
        };

        $scope.numSignatures = function () {
            return $scope.signatures.length
        };

        $scope.updateTimestamps = function (data) {
            $scope.$apply(function () {
                $scope.timestamp = JSON.parse(data.body);
                $scope.haveData = true;
                $scope.connected = true;
            });
        };

        $scope.updateSignatures = function (data) {
            var update = JSON.parse(data.body);
            var updateLen = update.signatures.length;

            $scope.$apply(function () {
                $scope.haveData = true;
                $scope.connected = true;
                $scope.progressMax = updateLen;
                $scope.progress = 0;
            });

            for (var i = 0; i < updateLen; i++) {
                var s = update.signatures[i];
                var found = false;

                for (var j = 0, len2 = $scope.signatures.length; j < len2; j++) {
                    if ($scope.signatures[j].name === s.name) {
                        $scope.$apply(function () {
                            $scope.progress = i;
                            $scope.signatures[j].invokedAtMillis = s.invokedAtMillis;
                            $scope.signatures[j].invokedAtString = s.invokedAtString;
                        });
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    $scope.$apply(function () {
                        $scope.progress = i;
                        $scope.signatures[$scope.signatures.length] = s;
                    });
                }
            }

            $scope.$apply(function () {
                $scope.progressMax = undefined;
                $scope.progress = undefined;
            });
        };

        $scope.disconnected = function () {
            console.log("Disconnected");
            $scope.connected = false;

            // Cannot use $location here, since /login is outside the Angular app
            $window.location.href = "/login?logout";
        };

        $scope.initSockets = function () {
            $scope.socket.client = new SockJS("/codekvast", null, {debug: true});
            $scope.socket.stomp = Stomp.over($scope.socket.client);

            $scope.socket.stomp.connect({}, function () {
                console.log("Connected");
                $scope.connected = true;
                $scope.socket.stomp.subscribe("/user/queue/timestamps", $scope.updateTimestamps);
                $scope.socket.stomp.subscribe("/app/signatures", $scope.updateSignatures);
                $scope.socket.stomp.subscribe("/user/queue/signatureUpdates", $scope.updateSignatures);
            }, function (error) {
                console.log("Cannot connect %o", error)
            });
            $scope.socket.client.onclose = $scope.disconnected;
        };

        $scope.initSockets();
    }])

    .filter('suppressEmptyDate', function () {
        return function (input) {
            return input == 0 ? "" : input;
        };
    });
