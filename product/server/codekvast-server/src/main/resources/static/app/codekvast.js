//noinspection JSUnusedGlobalSymbols
'use strict';

var codekvastApp = angular.module('codekvastApp', ['ngRoute', 'ui.bootstrap'])

    .config(['$routeProvider', '$locationProvider', 'Defaults', function ($routeProvider, $locationProvider, Defaults) {
        $routeProvider
            .when('/page/:page*', {
                templateUrl: function (routeParams) {
                    return "partials/" + routeParams.page + '.html'
                }
            })

            .otherwise({
                templateUrl: 'partials/' + Defaults.defaultRoute + '.html'
            });

        $locationProvider.html5Mode(true);
    }])

    .service('DateService', function () {
        var getDurationBetween = function (now, past) {
            var age = Math.abs(now - past);
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

        this.prettyAge = function (timestampMillis) {
            return getDurationBetween(timestampMillis, Date.now());
        };

        this.prettyDuration = function(timestampMillis) {
            return getDurationBetween(timestampMillis, 0);
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

        var onApplicationStatisticsMessage = function (message) {
            broadcast('applicationStatistics', JSON.parse(message.body));
        }

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
                socket.stomp.subscribe("/user/queue/application/statistics", onApplicationStatisticsMessage);
                socket.stomp.subscribe("/user/queue/collector/status", onCollectorStatusMessage);
                socket.stomp.subscribe("/user/queue/signature/data", onSignatureDataMessage);

                $http.get('/api/signatures')
                    .success(function (data) {
                        broadcast('jumbotronMessage', null);
                        broadcast('applicationStatistics', data.applicationStatistics);
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

        var persistsApplicationSettings = function (collectorStatus) {
            var data = {collectorSettings: []};
            for (var i = 0, len = collectorStatus.applications.length; i < len; i++) {
                var a = collectorStatus.applications[i];
                data.collectorSettings.push({
                    name: a.name,
                    usageCycleSeconds: a.usageCycleValue * a.usageCycleMultiplier
                })
            }

            $http.post('/api/collectorSettings', data)
                .success(function () {
                    console.log("Saved collector settings %o", data);
                })
                .error(function (rsp) {
                    console.log("Cannot save collector settings %o", rsp);
                })

        };

        return {
            getLastEvent: getLastEvent,
            initSocket: initSocket,
            getAllSignatures: getAllSignatures,
            persistsApplicationSettings: persistsApplicationSettings
        }
    }])

    .constant('Defaults', {defaultRoute: 'application-statistics'})


    .controller('NavigationController', ['$scope', '$location', '$modal', 'Defaults', function($scope, $location, $modal, Defaults) {
        $scope.menuItems = [
            {
                name: 'Application Statistics',
                url: '/page/application-statistics',
                title: 'Show collection status',
                icon: 'glyphicon-stats'
            },
            {
                name: 'Reports',
                url: '/page/reports',
                title: 'Generate reports of truly dead code',
                icon: 'glyphicon-th-list'
            }
        ];

        $scope.rightMenuItems = [
            {
                name: 'Collection Details',
                url: '/page/collectors',
                title: 'Shows detailed low-level status of the collectors',
                icon: 'glyphicon-dashboard'
            }
        ];

        $scope.isActive = function (viewLocation) {
            return viewLocation === $location.path() || (viewLocation === "/page/" + Defaults.defaultRoute && $location.path() === "/");
        };

        $scope.openSettings = function () {
            var modalInstance = $modal.open({
                templateUrl: 'partials/settings.html',
                controller: 'SettingsController'
            });
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

    .controller('SettingsController', ['$scope', '$modalInstance', 'StompService', 'DateService', function ($scope, $modalInstance, StompService, DateService) {
        $scope.collectorStatus = StompService.getLastEvent('collectorStatus');

        $scope.setUnit = function (a, code) {
            if (!a.usageCycleValue) {
                a.usageCycleValue = a.usageCycleSeconds;
                a.usageCycleMultiplier = 1;
            }
            a.usageCycleUnit = code;
            switch (code) {
                case 'seconds':
                    a.usageCycleValue = Math.max(1, Math.floor(a.usageCycleValue * a.usageCycleMultiplier));
                    a.usageCycleMultiplier = 1;
                    break;
                case 'minutes':
                    a.usageCycleValue = Math.max(1, Math.floor(a.usageCycleValue * a.usageCycleMultiplier / 60));
                    a.usageCycleMultiplier = 60;
                    break;
                case 'hours':
                    a.usageCycleValue = Math.max(1, Math.floor(a.usageCycleValue * a.usageCycleMultiplier / 60 / 60));
                    a.usageCycleMultiplier = 60 * 60;
                    break;
                case 'days':
                    a.usageCycleValue = Math.max(1, Math.floor(a.usageCycleValue * a.usageCycleMultiplier / 60 / 60 / 24));
                    a.usageCycleMultiplier = 60 * 60 * 24;
                    break;
                case 'months':
                    a.usageCycleValue = Math.max(1, Math.floor(a.usageCycleValue * a.usageCycleMultiplier / 60 / 60 / 24 / 30));
                    a.usageCycleMultiplier = 60 * 60 * 24 * 30;
                    break;
                case 'years':
                    a.usageCycleValue = Math.max(1, Math.floor(a.usageCycleValue * a.usageCycleMultiplier / 60 / 60 / 24 / 365));
                    a.usageCycleMultiplier = 60 * 60 * 24 * 365;
                    break;
            }
        };

        if ($scope.collectorStatus) {
            for (var i = 0, len = $scope.collectorStatus.applications.length; i < len; i++) {
                var a = $scope.collectorStatus.applications[i];
                var v = DateService.prettyDuration(a.usageCycleSeconds * 1000);
                if (v.endsWith('d')) {
                    $scope.setUnit(a, 'days');
                } else if (v.endsWith('h')) {
                    $scope.setUnit(a, 'hours');
                } else if (v.endsWith('m')) {
                    $scope.setUnit(a, 'minutes');
                } else {
                    $scope.setUnit(a, 'seconds');
                }
            }
        }

        $scope.$on('stompDisconnected', function (event, message) {
            $scope.collectorStatus = undefined;
        });

        $scope.save = function () {
            if ($scope.collectorStatus) {
                StompService.persistsApplicationSettings($scope.collectorStatus);
            }

            $modalInstance.close();
        };

        $scope.cancel = function () {
            $modalInstance.dismiss('cancel');
        };

    }])

    .controller('StatisticsController', ['$scope', '$interval', 'DateService', 'StompService', function ($scope, $interval, DateService, StompService) {
        $scope.applicationStatistics = StompService.getLastEvent('applicationStatistics');
        $scope.dateFormat = 'yyyy-MM-dd HH:mm:ss';

        $scope.$on('applicationStatistics', function (event, data) {
            $scope.applicationStatistics = data;
            $scope.updateModel();
        });

        $scope.updateModel = function () {
            if ($scope.applicationStatistics) {
                for (var i = 0, len = $scope.applicationStatistics.applications.length; i < len; i++) {
                    var a = $scope.applicationStatistics.applications[i];
                    a.usageCycle = DateService.prettyDuration(a.usageCycleSeconds * 1000);
                    a.timeToFullUsageCycle = DateService.prettyAge(a.firstDataReceivedAtMillis + a.usageCycleSeconds * 1000);
                    a.collectorAge = DateService.prettyAge(a.firstDataReceivedAtMillis);
                    a.inUseSeconds = Math.floor((Date.now() - a.firstDataReceivedAtMillis) / 1000);
                    a.inUse = DateService.prettyDuration(a.inUseSeconds * 1000);
                    a.percentOfUsageCycle = Math.floor(a.inUseSeconds * 100 / a.usageCycleSeconds);
                    a.usageCycleProgressType = a.percentOfUsageCycle < 10 ? 'danger' : 'warning';
                    a.leftCompletedBarWidth = Math.floor(a.usageCycleSeconds * 100 / a.inUseSeconds);
                    a.rightCompletedBarWidth = 100 - a.leftCompletedBarWidth;
                    a.usageCycleMultiple = Math.round(a.inUseSeconds / a.usageCycleSeconds * 10) / 10;
                    if (a.usageCycleMultiple >= 10) {
                        a.usageCycleMultiple = Math.round(a.inUseSeconds / a.usageCycleSeconds);
                    }
                    if (a.fullUsageCycleElapsed) {
                        a.trulyDeadTooltip = "This is truly dead code";
                    } else {
                        a.numTrulyDeadSignatures = "?"
                        a.percentTrulyDeadSignatures = "?"
                        a.trulyDeadTooltip = "Be patient for another " + a.timeToFullUsageCycle + " ...";
                    }
                    a.dataAge = DateService.prettyAge(a.lastDataReceivedAtMillis);
                    a.collectorsWorkingType = a.collectorsWorking === "all" ? "success" : a.collectorsWorking === "some" ? 'warning' : 'danger';
                }
            }
        }

        $scope.$on('stompDisconnected', function (event, message) {
            $scope.applicationStatistics = undefined;
            $interval.cancel($scope.updateModelInterval);
        });

        $scope.updateModelInterval = $interval($scope.updateModel, 500, false);
    }])

    .controller('CollectorController', ['$scope', '$interval', 'DateService', 'StompService', function ($scope, $interval, DateService, StompService) {
        $scope.collectorStatus = StompService.getLastEvent('collectorStatus');
        $scope.collectorStatusOpen = true;
        $scope.dateFormat = 'yyyy-MM-dd HH:mm:ss';

        $scope.$on('collectorStatus', function (event, data) {
            $scope.collectorStatus = data;
            $scope.updateModel();
        });

        $scope.$on('stompDisconnected', function (event, message) {
            $scope.collectorStatus = undefined;

            $interval.cancel($scope.updateModelInterval);
        });

        $scope.updateModel = function () {
            if ($scope.collectorStatus) {
                for (var i = 0, len = $scope.collectorStatus.collectors.length; i < len; i++) {
                    var c = $scope.collectorStatus.collectors[i];
                    c.collectorResolution = DateService.prettyDuration(c.collectorResolutionSeconds * 1000);
                    c.agentUploadInterval = DateService.prettyDuration(c.agentUploadIntervalSeconds * 1000);
                    c.collectorAge = DateService.prettyAge(c.startedAtMillis);
                    c.dataAge = DateService.prettyAge(c.dataReceivedAtMillis);
                }
            }
        };

        $scope.updateModelInterval = $interval($scope.updateModel, 500, false);

    }])

    .controller('SignatureController', ['$scope', '$filter', 'StompService', function ($scope, $filter, StompService) {

        $scope.newestSignatures = undefined;
        $scope.newestSignaturesOpen = false;
        $scope.trulyDeadSignatures = undefined;
        $scope.trulyDeadSignaturesOpen = false;
        $scope.dateFormat = 'yyyy-MM-dd HH:mm:ss';

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
