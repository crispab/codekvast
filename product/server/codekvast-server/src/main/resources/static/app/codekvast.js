//noinspection JSUnusedGlobalSymbols
'use strict';

var codekvastApp = angular.module('codekvastApp', ['ui.bootstrap'])

    .config(['$locationProvider', function ($locationProvider) {
        $locationProvider.html5Mode(true);
    }])

    .service('DateService', function () {
        this.getAgeSince = function (now, timestamp) {
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

    .factory('StompService', ['$rootScope', '$http', '$timeout', function ($rootScope, $http, $timeout) {
        var socket = {client: null, stomp: null};

        var broadcast = function (event, message) {
            $timeout(function () {
                $rootScope.$broadcast(event, message);
            }, 0);
        };

        var broadcastSignatures = function (collectorStatus, signatures) {
            broadcast('signatures', {
                first: collectorStatus != null,
                collectorStatus: collectorStatus,
                signatures: signatures
            });
        };

        var onCollectorStatusMessage = function (data) {
            broadcast('collectorStatus', JSON.parse(data.body));
        };

        var onSignatureDataMessage = function (message) {
            var signatureDataMessage = JSON.parse(message.body);
            broadcastSignatures(null, signatureDataMessage.signatures);
        };

        var onConnected = function () {
            console.log("Connected");
            broadcast('stompConnected');
            broadcast('stompStatus', 'Waiting for data...');
        };

        var onDisconnect = function (message) {
            console.log("Disconnected");
            broadcast('stompDisconnected', message);
        };

        var initSocket = function () {
            socket.client = new SockJS("/codekvast", null, {debug: true});
            socket.stomp = Stomp.over(socket.client);

            socket.stomp.connect({}, function () {
                onConnected();
                socket.stomp.subscribe("/user/queue/collector/status", onCollectorStatusMessage);
                socket.stomp.subscribe("/user/queue/signature/data", onSignatureDataMessage);
                $http.get('/api/signatures')
                    .success(function (data) {
                        broadcastSignatures(data.collectorStatus, data.signatures);
                    })
                    .error(function (data) {
                        console.log("Cannot get signatures %o", data);
                        onDisconnect(data.toString());
                    })
            }, function (error) {
                console.log("Cannot connect %o", error);
                onDisconnect(error.toString());
            });
            socket.client.onclose = onDisconnect;
        };

        return {
            initSocket: initSocket
        }
    }])

    .controller('MainController', ['$scope', '$window', '$interval', 'DateService', function ($scope, $window, $interval, DateService) {
        $scope.jumbotronMessage = 'Disconnected from server';

        $scope.signatures = undefined;
        $scope.collectorStatus = undefined;
        $scope.statusPanelOpen = true;

        $scope.orderByInvokedAt = function () {
            $scope.sortField = ['invokedAtMillis', 'name'];
        };

        $scope.orderByName = function () {
            $scope.sortField = ['name', 'invokedAtMillis'];
        };

        $scope.orderByInvokedAt();

        $scope.maxRows = 100;

        $scope.reverse = false;

        $scope.updateAges = function () {
            if ($scope.collectorStatus) {
                for (var i = 0, len = $scope.collectorStatus.collectors.length; i < len; i++) {
                    var c = $scope.collectorStatus.collectors[i];
                    c.trulyDeadAfter = DateService.getAgeSince(c.trulyDeadAfterSeconds * 1000, 0);
                    c.collectorAge = DateService.getAge(c.collectorStartedAtMillis);
                    c.canReportIn = DateService.getAgeSince(c.collectorStartedAtMillis + c.trulyDeadAfterSeconds * 1000, Date.now());
                    c.updateAge = DateService.getAge(c.updateReceivedAtMillis);
                }
            }
        };

        $scope.updateAgeInterval = $interval($scope.updateAges, 500, false);

        $scope.$on('signatures', function (event, message) {

            $scope.jumbotronMessage = undefined;

            if (message.first) {
                $scope.collectorStatus = message.collectorStatus;
                $scope.signatures = message.signatures;
                $scope.updateAges();
                return;
            }

            var startedAt = Date.now();
            var signatures = message.signatures, updateLen = message.signatures.length;

            for (var i = 0; i < updateLen; i++) {
                var newSig = signatures[i];
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
            var elapsed = Date.now() - startedAt;
            console.log("Updated " + updateLen + " signatures in " + elapsed + " ms");
        });

        $scope.$on('collectorStatus', function (event, data) {
            $scope.jumbotronMessage = undefined;
            $scope.collectorStatus = data;
            $scope.updateAges();
        });

        $scope.$on('stompStatus', function (event, message) {
            $scope.jumbotronMessage = message;
        });

        $scope.$on('stompConnected', function () {
            $scope.jumbotronMessage = "Connected";
        });

        $scope.$on('stompDisconnected', function (event, message) {
            $scope.jumbotronMessage = message || "Disconnected";
            $scope.signatures = [];
            $scope.collectorStatus = undefined;

            $interval.cancel($scope.updateAgeInterval);

            // Cannot use $location here, since /login is outside the Angular app
            $window.location.href = "/login?logout";
        });
    }])

    .run(['StompService', function (StompService) {
        StompService.initSocket();
    }]);

