//noinspection JSUnusedGlobalSymbols
var codekvastApp = angular.module('codekvastApp', ['ui.bootstrap'])

    .config(['$locationProvider', function ($locationProvider) {
        $locationProvider.html5Mode(true);
    }])

    .controller('MainController', ['$scope', '$window', '$interval', function ($scope, $window, $interval) {
        $scope.jumbotronMessage = 'Disconnected from server';
        $scope.progress = undefined;

        $scope.signatures = [];
        $scope.collectorStatus = undefined;
        $scope.statusPanelOpen = true;

        $scope.showSignatures = function () {
            return $scope.collectorStatus && $scope.signatures.length > 0;
        };

        $scope.orderByInvokedAt = function () {
            $scope.sortField = ['invokedAtMillis', 'name'];
        };

        $scope.orderByName = function () {
            $scope.sortField = ['name', 'invokedAtMillis'];
        };

        $scope.orderByInvokedAt();

        $scope.maxRows = 100;

        $scope.reverse = false;

        $scope.socket = {
            client: null,
            stomp: null
        };

        onCollectorStatusMessage = function (data) {
            var collectorStatusMessage = JSON.parse(data.body);
            $scope.collectorStatus = collectorStatusMessage;
        };

        updateAges = function () {
            if ($scope.collectorStatus) {
                var now = Date.now();
                $scope.collectorStatus.collectionAge = getAge(now, $scope.collectorStatus.collectionStartedAtMillis);
                $scope.collectorStatus.updateAge = getAge(now, $scope.collectorStatus.updateReceivedAtMillis);
                for (var i = 0, len = $scope.collectorStatus.collectors.length; i < len; i++) {
                    var c = $scope.collectorStatus.collectors[i];
                    c.collectorAge = getAge(now, c.collectorStartedAtMillis);
                    c.updateAge = getAge(now, c.updateReceivedAtMillis);
                }
                $scope.$apply();
            }
        };

        updateAgeInterval = $interval(updateAges, 500, false);

        getAge = function (now, timestamp) {
            if (timestamp === 0) {
                return "";
            }

            var age = now - timestamp;
            var minute = 60 * 1000;
            if (age < 60 * minute) {
                return Math.round(age / minute) + " min";
            }
            var hour = minute * 60;
            if (age < 24 * hour) {
                return Math.round(age / hour) + " hours";
            }
            var day = hour * 24;
            if (age < 7 * day) {
                return Math.round(age / day) + " days";
            }
            var week = day * 7;
            return Math.round(age / week) + " weeks";
        };

        onSignatureMessage = function (data) {
            var signatureMessage = JSON.parse(data.body);

            if (signatureMessage.collectorStatus) {
                $scope.jumbotronMessage = undefined;
                $scope.collectorStatus = signatureMessage.collectorStatus;
            }

            $scope.progress = signatureMessage.progress;

            var updateLen = signatureMessage.signatures.length;
            for (var i = 0; i < updateLen; i++) {
                var newSig = signatureMessage.signatures[i];
                var found = false;

                for (var j = 0, len2 = $scope.signatures.length; j < len2; j++) {
                    var oldSig = $scope.signatures[j];
                    if (oldSig.name === newSig.name) {
                        found = true;
                        if (oldSig.invokedAtMillis < newSig.invokedAtMillis) {
                            oldSig.invokedAtMillis = newSig.invokedAtMillis;
                            oldSig.invokedAtString = newSig.invokedAtString;
                        }
                        break;
                    }
                }

                if (!found) {
                    $scope.signatures[$scope.signatures.length] = newSig;
                }

            }

            $scope.$apply();
        };

        $scope.disconnected = function () {
            console.log("Disconnected");
            $scope.$apply(function () {
                $scope.jumbotronMessage = "Disconnected";
                $scope.signatures = [];
                $scope.collectorStatus = undefined;
            });

            $interval.cancel(updateAgeInterval);

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
                $scope.socket.stomp.subscribe("/user/queue/collectorStatus", onCollectorStatusMessage);
                $scope.socket.stomp.subscribe("/app/signatures", onSignatureMessage);
                $scope.socket.stomp.subscribe("/user/queue/signatureUpdates", onSignatureMessage);
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

