//noinspection JSUnusedGlobalSymbols
var codekvastApp = angular.module('codekvastApp', ['ui.bootstrap'])

    .config(['$locationProvider', function ($locationProvider) {
        $locationProvider.html5Mode(true);
    }])

    .controller('MainCtrl', ['$scope', '$window', function ($scope, $window) {
        $scope.jumbotronMessage = 'Disconnected from server';
        $scope.progress = undefined;

        $scope.signatures = [];
        $scope.collectorStatus = undefined;

        $scope.reverse = false;

        $scope.haveSignatures = function () {
            return $scope.signatures.length > 0;
        }

        $scope.orderByInvokedAt = function () {
            $scope.sortField = ['invokedAtMillis', 'name'];
        };

        $scope.orderByName = function () {
            $scope.sortField = ['name', 'invokedAtMillis'];
        };

        $scope.orderByInvokedAt();

        $scope.maxRows = 100;

        $scope.socket = {
            client: null,
            stomp: null
        };

        $scope.updateCollectorStatus = function (data) {
            $scope.$apply(function () {
                var collectorStatusMessage = JSON.parse(data.body);
                $scope.collectorStatus = collectorStatusMessage;
            });
        };

        $scope.updateSignatures = function (data) {
            var signatureMessage = JSON.parse(data.body);

            $scope.$apply(function () {
                if (signatureMessage.collectorStatus) {
                    $scope.jumbotronMessage = undefined;
                    $scope.collectorStatus = signatureMessage.collectorStatus;
                }
                $scope.progress = signatureMessage.progress;
            });

            var updateLen = signatureMessage.signatures.length;
            for (var i = 0; i < updateLen; i++) {
                var s = signatureMessage.signatures[i];
                var found = false;

                for (var j = 0, len2 = $scope.signatures.length; j < len2; j++) {
                    if ($scope.signatures[j].name === s.name) {
                        $scope.signatures[j].invokedAtMillis = s.invokedAtMillis;
                        $scope.signatures[j].invokedAtString = s.invokedAtString;
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    $scope.signatures[$scope.signatures.length] = s;
                }

            }

            $scope.$apply(function () {
                $scope.progress = undefined;
            });
        };

        $scope.disconnected = function () {
            console.log("Disconnected");
            $scope.$apply(function () {
                $scope.jumbotronMessage = "Disconnected";
                $scope.signatures = [];
                $scope.collectorStatus = undefined;
            });

            // Cannot use $location here, since /login is outside the Angular app
            $window.location.href = "/login?logout";
        };

        $scope.initSockets = function () {
            $scope.socket.client = new SockJS("/codekvast", null, {debug: true});
            $scope.socket.stomp = Stomp.over($scope.socket.client);

            $scope.socket.stomp.connect({}, function () {
                console.log("Connected");
                $scope.jumbotronMessage = 'Waiting for data...';
                $scope.$apply();
                $scope.socket.stomp.subscribe("/user/queue/collectorStatus", $scope.updateCollectorStatus);
                $scope.socket.stomp.subscribe("/app/signatures", $scope.updateSignatures);
                $scope.socket.stomp.subscribe("/user/queue/signatureUpdates", $scope.updateSignatures);
            }, function (error) {
                console.log("Cannot connect %o", error)
                $scope.$apply(function () {
                    $scope.jumbotronMessage = error.toString();
                });
            });
            $scope.socket.client.onclose = $scope.disconnected;
        };

        $scope.initSockets();
    }]);

