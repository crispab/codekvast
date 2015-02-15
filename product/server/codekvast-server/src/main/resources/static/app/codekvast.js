//noinspection JSUnusedGlobalSymbols
var codekvastApp = angular.module('codekvastApp', ['ui.bootstrap'])

    .config(['$locationProvider', function ($locationProvider) {
        $locationProvider.html5Mode(true);
    }])

    .service('DateService', function () {
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

    .service('StompService', ['$rootScope', function ($rootScope) {
        this.receptionInProgress = false;

        this.socket = {
            client: null,
            stomp: null
        };

        this.onCollectorStatusMessage = function (data) {
            $rootScope.$broadcast('collectorStatus', JSON.parse(data.body));
        };

        this.onSignaturesAvailableMessage = function (message) {
            var self = this;
            var signaturesAvailableMessage = JSON.parse(message.body);
            if (signaturesAvailableMessage.pendingSignatures > 0) {
                if (!self.receptionInProgress) {
                    $rootScope.$broadcast('signatureReceptionInProgress', signaturesAvailableMessage.progress);
                }
                self.receptionInProgress = true;
                self.socket.stomp.send("/user/queue/signature/next")
            }
        };

        this.onSignatureDataMessage = function (message) {
            var signatureDataMessage = JSON.parse(message.body);
            $rootScope.$broadcast('signatureReceptionInProgress', signatureDataMessage.progress);
            $rootScope.$broadcast('signatures', signatureDataMessage.signatures);
            if (signatureDataMessage.more) {
                self.socket.stomp.send("/user/queue/signature/next")
            } else {
                self.receptionInProgress = false;
            }
            // message.ack();
        };

        this.onConnected = function () {
            console.log("Connected");
            $rootScope.$broadcast('stompConnected');
            $rootScope.$broadcast('stompStatus', 'Waiting for data...');
        };

        this.onDisconnect = function (message) {
            console.log("Disconnected");
            $rootScope.$broadcast('stompDisconnected', message);
        };

        this.initSocket = function () {
            var self = this;
            self.socket.client = new SockJS("/codekvast", null, {debug: true});
            self.socket.stomp = Stomp.over(self.socket.client);

            self.socket.stomp.connect({}, function () {
                self.onConnected();
                self.socket.stomp.subscribe("/user/queue/collector/status", self.onCollectorStatusMessage);
                self.socket.stomp.subscribe("/user/queue/signature/available", self.onSignaturesAvailableMessage);
                self.socket.stomp.subscribe("/user/queue/signature/data", self.onSignatureDataMessage); //, {ack: 'client'});
                self.socket.stomp.send("/topic/hello", {}, "Hi there, give me my signatures!");
            }, function (error) {
                console.log("Cannot connect %o", error)
                self.onDisconnect(error.toString());
            });
            self.socket.client.onclose = self.onDisconnect;
        };
    }])

    .controller('MainController', ['$scope', '$window', '$interval', 'DateService', function ($scope, $window, $interval, DateService) {
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

        updateAges = function () {
            if ($scope.collectorStatus) {
                $scope.collectorStatus.collectionAge = DateService.getAge($scope.collectorStatus.collectionStartedAtMillis);
                $scope.collectorStatus.updateAge = DateService.getAge($scope.collectorStatus.updateReceivedAtMillis);
                for (var i = 0, len = $scope.collectorStatus.collectors.length; i < len; i++) {
                    var c = $scope.collectorStatus.collectors[i];
                    c.collectorAge = DateService.getAge(c.collectorStartedAtMillis);
                    c.updateAge = DateService.getAge(c.updateReceivedAtMillis);
                }
                $scope.$apply();
            }
        };

        updateAgeInterval = $interval(updateAges, 500, false);

        $scope.$on('signatures', function (event, signatureMessage) {

            if (signatureMessage.collectorStatus) {
                $scope.jumbotronMessage = undefined;
                $scope.collectorStatus = signatureMessage.collectorStatus;
            }

            $scope.$apply(function () {
                $scope.progress = signatureMessage.progress;
            });

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
        });

        $scope.$on('collectorStatus', function (event, data) {
            $scope.jumbotronMessage = undefined;
            $scope.collectorStatus = data;
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

            $interval.cancel(updateAgeInterval);

            // Cannot use $location here, since /login is outside the Angular app
            $window.location.href = "/login?logout";
        });
    }])

    .run(['StompService', function (StompService) {
        StompService.initSocket();
    }]);

