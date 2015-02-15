//noinspection JSUnusedGlobalSymbols
var codekvastApp = angular.module('codekvastApp', ['ui.bootstrap'])

    .config(['$locationProvider', function ($locationProvider) {
        $locationProvider.html5Mode(true);
    }])

    .service('dateService', function () {
        this.getAgeSince = function (now, timestamp) {
            if (timestamp === 0) {
                return "";
            }

            var age = now - timestamp;
            var second = 1000;
            var minute = second * 60;
            var hour = minute * 60;
            var day = hour * 24;

            var result = "";
            if (age >= day) {
                var days = Math.floor(age / day);
                age = age - days * day;
                result = result + days + "d ";
            }
            if (age >= hour) {
                var hours = Math.floor(age / hour);
                age = age - hours * hour;
                result = result + hours + "h ";
            }
            if (age >= minute) {
                var minutes = Math.floor(age / minute);
                age = age - minutes * minute;
                result = result + minutes + "m ";
            }
            if (age >= second) {
                var seconds = Math.floor(age / second);
                age = age - seconds * second;
                result = result + seconds + "s";
            }
            return result.trim();
        };

        this.getAge = function (timestamp) {
            return this.getAgeSince(Date.now(), timestamp);
        }
    })

    .controller('MainController', ['$scope', '$window', '$interval', 'dateService', function ($scope, $window, $interval, dateService) {
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
                $scope.collectorStatus.collectionAge = dateService.getAge($scope.collectorStatus.collectionStartedAtMillis);
                $scope.collectorStatus.updateAge = dateService.getAge($scope.collectorStatus.updateReceivedAtMillis);
                for (var i = 0, len = $scope.collectorStatus.collectors.length; i < len; i++) {
                    var c = $scope.collectorStatus.collectors[i];
                    c.collectorAge = dateService.getAge(c.collectorStartedAtMillis);
                    c.updateAge = dateService.getAge(c.updateReceivedAtMillis);
                }
                $scope.$apply();
            }
        };

        updateAgeInterval = $interval(updateAges, 500, false);

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

