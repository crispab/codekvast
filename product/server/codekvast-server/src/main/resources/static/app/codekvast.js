//noinspection JSUnusedGlobalSymbols
'use strict';

var codekvastApp = angular.module('codekvastApp', ['ngRoute', 'ui.bootstrap'])

    .config(['$routeProvider', '$locationProvider', function ($routeProvider, $locationProvider) {
        $routeProvider
            .when('/page/:page*', {
                templateUrl: function (routeParams) {
                    return "partials/" + routeParams.page + '.html'
                }
            })

            .otherwise({
                templateUrl: 'partials/welcome.html'
            });


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
        var lastMessages = {};
        var allSignatures = [];

        var broadcast = function (event, message) {
            lastMessages[event] = message;

            $timeout(function () {
                $rootScope.$broadcast(event, message);
            }, 0);
        };

        var getLastEvent = function (event) {
            return lastMessages[event];
        };

        var getAllSignatures = function () {
            return allSignatures;
        };

        var updateSignatures = function (signatures) {
            var startedAt = Date.now();
            var updateLen = signatures.length;

            for (var i = 0; i < updateLen; i++) {
                var newSig = signatures[i];
                var found = false;

                for (var j = 0, len2 = allSignatures.length; j < len2; j++) {
                    var oldSig = allSignatures[j];
                    if (oldSig.name === newSig.name) {
                        found = true;
                        if (oldSig.invokedAtMillis < newSig.invokedAtMillis) {
                            oldSig.invokedAtMillis = newSig.invokedAtMillis;
                        }
                        break;
                    }
                }

                if (!found) {
                    allSignatures[allSignatures.length] = newSig;
                }
            }
            var elapsed = Date.now() - startedAt;
            console.log("Updated " + updateLen + " signatures in " + elapsed + " ms");
        };

        var onCollectorStatusMessage = function (message) {
            broadcast('collectorStatus', JSON.parse(message.body));
        };

        var onSignatureDataMessage = function (message) {
            var signatureDataMessage = JSON.parse(message.body);
            updateSignatures(signatureDataMessage.signatures);
            broadcast('signatures');
        };

        var onConnected = function () {
            console.log("Connected");
            broadcast('stompConnected');
            broadcast('jumbotronMessage', 'Waiting for data...');
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
                        broadcast('jumbotronMessage', null);
                        broadcast('collectorStatus', data.collectorStatus);
                        allSignatures = data.signatures;
                        broadcast('signatures');
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
            getLastEvent: getLastEvent,
            initSocket: initSocket,
            getAllSignatures: getAllSignatures
        }
    }])

    .controller('JumbotronController', ['$scope', '$window', 'StompService', function ($scope, $window, StompService) {
        $scope.jumbotronMessage = StompService.getLastEvent('jumbotronMessage');

        $scope.$on('jumbotronMessage', function (event, message) {
            $scope.jumbotronMessage = message;
        });

        $scope.$on('stompConnected', function () {
            $scope.jumbotronMessage = "Connected";
        });

        $scope.$on('stompDisconnected', function (event, message) {
            $scope.jumbotronMessage = message || "Disconnected";

            // Cannot use $location here, since /login is outside the Angular app
            $window.location.href = "/login?logout";
        });

    }])

    .controller('CollectorController', ['$scope', '$interval', 'DateService', 'StompService', function ($scope, $interval, DateService, StompService) {
        $scope.collectorStatus = StompService.getLastEvent('collectorStatus');
        $scope.collectorStatusOpen = false;
        $scope.dateFormat = 'short';

        $scope.$on('collectorStatus', function (event, data) {
            $scope.collectorStatus = data;
            $scope.updateAges();
        });

        $scope.$on('stompDisconnected', function (event, message) {
            $scope.collectorStatus = undefined;

            $interval.cancel($scope.updateAgeInterval);
        });

        $scope.updateAges = function () {
            if ($scope.collectorStatus) {
                for (var i = 0, len = $scope.collectorStatus.collectors.length; i < len; i++) {
                    var c = $scope.collectorStatus.collectors[i];
                    c.trulyDeadAfter = DateService.getAgeSince(c.trulyDeadAfterSeconds * 1000, 0);
                    c.collectorAge = DateService.getAge(c.startedAtMillis);
                    c.countDown = DateService.getAgeSince(c.startedAtMillis + c.trulyDeadAfterSeconds * 1000, Date.now());
                    c.updateAge = DateService.getAge(c.dataReceivedAtMillis);
                }
            }
        };

        $scope.updateAgeInterval = $interval($scope.updateAges, 500, false);

    }])

    .controller('SignatureController', ['$scope', '$filter', 'StompService', function ($scope, $filter, StompService) {

        $scope.newestSignatures = undefined;
        $scope.newestSignaturesOpen = false;
        $scope.trulyDeadSignatures = undefined;
        $scope.trulyDeadSignaturesOpen = false;

        $scope.filter = {
            minAgeValue: 30,
            signature: undefined,
            maxRows: 100
        };

        $scope.setAgeUnit = function (code) {
            switch (code) {
                case 0:
                    $scope.filter.ageUnit = 'minutes';
                    $scope.filter.ageStep = 15;
                    $scope.filter.ageMultiplier = 60 * 1000;
                    break;
                case 1:
                    $scope.filter.ageUnit = 'hours';
                    $scope.filter.ageStep = 6;
                    $scope.filter.ageMultiplier = 60 * 60 * 1000;
                    break;
                case 2:
                    $scope.filter.ageUnit = 'days';
                    $scope.filter.ageStep = 1;
                    $scope.filter.ageMultiplier = 24 * 60 * 60 * 1000;
                    break;
            }
        };

        $scope.setAgeUnit(0);

        $scope.setFilteredSignatures = function() {
            var minAgeMillis = Date.now() - $scope.filter.minAgeValue * $scope.filter.ageMultiplier;
            var filtered = $scope.allSignatures;
            filtered = $filter('filter')(filtered, $scope.filter.signature);
            filtered = $filter('filter')(filtered, function (s) {
                return s.invokedAtMillis < minAgeMillis;
            });
            filtered = $filter('orderBy')(filtered, 'invokedAtMillis');
            filtered = $filter('limitTo')(filtered, $scope.filter.maxRows);
            $scope.trulyDeadSignatures = filtered;

            filtered = $scope.allSignatures;
            filtered = $filter('orderBy')(filtered, 'invokedAtMillis', true);
            filtered = $filter('limitTo')(filtered, 10);
            $scope.newestSignatures = filtered;
        };

        $scope.$watchCollection('filter', $scope.setFilteredSignatures);

        $scope.allSignatures = StompService.getAllSignatures();
        $scope.setFilteredSignatures();

        $scope.$on('signatures', function (event) {
            $scope.allSignatures = StompService.getAllSignatures();
            $scope.setFilteredSignatures();
        });

        $scope.$on('stompDisconnected', function (event, message) {
            $scope.signatures = undefined;
        });
    }])

    .filter('invokedAtDate', ['dateFilter', function (dateFilter) {
        return function (input, format) {
            if (!input || input === 0) {
                return "";
            }
            return dateFilter(input, format);
        }
    }])

    .run(['StompService', function (StompService) {
        StompService.initSocket();
    }]);

