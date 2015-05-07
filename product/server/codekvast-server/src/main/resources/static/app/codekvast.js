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

    .factory('RemoteDataService', ['$rootScope', '$http', '$timeout', function ($rootScope, $http, $timeout) {
        var socket = {client: null, stomp: null};
        var lastMessages = {};

        var broadcast = function (event, message) {
            lastMessages[event] = message;

            $timeout(function () {
                $rootScope.$broadcast(event, message);
            }, 0);
        };

        var getLastEvent = function (event) {
            return lastMessages[event];
        };

        var getLastData = function (what) {
            var data = lastMessages['data']
            return data ? data[what] : undefined;
        };

        var handleWebSocketMessage = function (data) {
            broadcast('data', data);
        }

        var onWebSocketMessage = function (message) {
            handleWebSocketMessage(JSON.parse(message.body))
        }

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
                socket.stomp.subscribe("/user/queue/data", onWebSocketMessage);

                $http.get('/api/web/data')
                    .success(function (data) {
                        broadcast('jumbotronMessage', null);
                        handleWebSocketMessage(data);
                    })
                    .error(function (data) {
                        console.log("Cannot get data %o", data);
                        onDisconnect(data.toString());
                    })
            }, function (error) {
                console.log("Cannot connect %o", error);
                onDisconnect(error.toString());
            });
            socket.client.onclose = onDisconnect;
        };

        var persistsOrganisationSettings = function (applications) {
            var data = {applicationSettings: []};
            for (var i = 0, len = applications.length; i < len; i++) {
                var a = applications[i];
                data.applicationSettings.push({
                    name: a.name,
                    usageCycleSeconds: a.usageCycleValue * a.usageCycleMultiplier
                })
            }

            $http.post('/api/web/settings', data)
                .success(function () {
                    console.log("Saved settings %o", data);
                })
                .error(function (rsp) {
                    console.log("Cannot save settings %o", rsp);
                })

        };

        return {
            getLastEvent: getLastEvent,
            getLastData: getLastData,
            initSocket: initSocket,
            persistsOrganisationSettings: persistsOrganisationSettings
        }
    }])

    .constant('Defaults', {defaultRoute: 'application-statistics'})


    .controller('NavigationController', ['$scope', '$location', '$modal', 'Defaults', function($scope, $location, $modal, Defaults) {
        $scope.menuItems = [
            {
                name: 'Application Usage Statistics',
                url: '/page/application-statistics',
                title: 'Show collection status',
                icon: 'glyphicon-stats'
            },
            {
                name: 'Generate Code Usage Report',
                url: '/page/code-usage-report',
                title: 'Generate reports of code usage',
                icon: 'glyphicon-th-list'
            }
        ];

        $scope.rightMenuItems = [
            {
                name: 'Collector Details',
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

    .controller('JumbotronController', ['$scope', '$window', 'RemoteDataService', function ($scope, $window, RemoteDataService) {
        $scope.jumbotronMessage = RemoteDataService.getLastEvent('jumbotronMessage');

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

    .controller('SettingsController', ['$scope', '$modalInstance', 'RemoteDataService', 'DateService', function ($scope, $modalInstance, RemoteDataService, DateService) {
        $scope.applications = RemoteDataService.getLastData("applications");

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

        if ($scope.applications) {
            for (var i = 0, len = $scope.applications.length; i < len; i++) {
                var a = $scope.applications[i];
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
            $scope.applications = undefined;
        });

        $scope.save = function () {
            if ($scope.applications) {
                RemoteDataService.persistsOrganisationSettings($scope.applications);
            }

            $modalInstance.close();
        };

        $scope.cancel = function () {
            $modalInstance.dismiss('cancel');
        };

    }])

    .controller('StatisticsController', ['$scope', '$interval', 'DateService', 'RemoteDataService', function ($scope, $interval, DateService, RemoteDataService) {
        $scope.applicationStatistics = RemoteDataService.getLastData("applicationStatistics");
        $scope.dateFormat = 'yyyy-MM-dd HH:mm:ss';
        $scope.orderBy = 'name';
        $scope.reverse = false;

        $scope.$on('data', function (event, data) {
            $scope.applicationStatistics = data.applicationStatistics;
            $scope.updateModel();
        });

        $scope.updateModel = function () {
            if ($scope.applicationStatistics) {
                for (var i = 0, len = $scope.applicationStatistics.length; i < len; i++) {
                    var a = $scope.applicationStatistics[i];
                    a.usageCycle = DateService.prettyDuration(a.usageCycleSeconds * 1000);
                    a.timeToFullUsageCycle = DateService.prettyDuration((a.usageCycleSeconds - a.upTimeSeconds) * 1000);
                    a.collectorAge = DateService.prettyAge(a.firstDataReceivedAtMillis);
                    a.upTime = DateService.prettyDuration(a.upTimeSeconds * 1000);
                    a.percentOfUsageCycle = Math.floor(a.upTimeSeconds * 100 / a.usageCycleSeconds);
                    a.usageCycleProgressType = a.percentOfUsageCycle < 10 ? 'danger' : 'warning';
                    a.leftCompletedBarWidth = Math.floor(a.usageCycleSeconds * 100 / a.upTimeSeconds);
                    a.rightCompletedBarWidth = 100 - a.leftCompletedBarWidth;
                    a.usageCycleMultiple = Math.round(a.upTimeSeconds / a.usageCycleSeconds * 10) / 10;
                    if (a.usageCycleMultiple >= 10) {
                        a.usageCycleMultiple = Math.round(a.upTimeSeconds / a.usageCycleSeconds);
                    }
                    if (a.fullUsageCycleCompleted) {
                        a.percentPossiblyDead = a.percentPossiblyDeadSignatures + "%"
                        a.possiblyDeadTooltip = "This is possibly dead code";
                    } else {
                        a.percentPossiblyDead = undefined
                        a.possiblyDeadTooltip = "Be patient for another " + a.timeToFullUsageCycle + " ...";
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

    .controller('CollectorsController', ['$scope', '$interval', 'DateService', 'RemoteDataService', function ($scope, $interval, DateService, RemoteDataService) {
        $scope.collectorStatuses = RemoteDataService.getLastData("collectors");
        $scope.dateFormat = 'yyyy-MM-dd HH:mm:ss';

        $scope.$on('data', function (event, data) {
            $scope.collectorStatuses = data.collectors;
            $scope.updateModel();
        });

        $scope.$on('stompDisconnected', function (event, message) {
            $scope.collectorStatuses = undefined;

            $interval.cancel($scope.updateModelInterval);
        });

        $scope.dataAgeClass = function (c) {
            return $scope.isDown(c) ? "danger" : undefined;
        };

        $scope.isDown = function (c) {
            var ageSeconds = (Date.now() - c.dataReceivedAtMillis) / 1000;
            return ageSeconds > c.collectorResolutionSeconds + c.agentUploadIntervalSeconds + 30;
        }

        $scope.updateModel = function () {
            if ($scope.collectorStatuses) {
                for (var i = 0, len = $scope.collectorStatuses.length; i < len; i++) {
                    var c = $scope.collectorStatuses[i];
                    c.collectorResolution = DateService.prettyDuration(c.collectorResolutionSeconds * 1000);
                    c.agentUploadInterval = DateService.prettyDuration(c.agentUploadIntervalSeconds * 1000);
                    c.collectorAge = DateService.prettyAge(c.startedAtMillis);
                    c.dataAge = DateService.prettyAge(c.dataReceivedAtMillis);
                }
            }
        };

        $scope.updateModelInterval = $interval($scope.updateModel, 500, false);

    }])

    .controller('ReportController', ['$scope', 'DateService', 'RemoteDataService', function ($scope, DateService, RemoteDataService) {
        $scope.apps = RemoteDataService.getLastData("applications")

        $scope.$on('data', function (event, data) {
            $scope.apps = data.applications;
            $scope.updateModel();
        });

        $scope.updateModel = function() {
            if ($scope.apps) {
                angular.forEach($scope.apps, function (a) {
                    a.usageCycle = DateService.prettyDuration(a.usageCycleSeconds * 1000);
                    if (a.selected === undefined) {
                        a.selected = true;
                    }
                });
            }
        };

        $scope.updateModel();

        $scope.formData = {
            applications: $scope.apps,

            versions: [
                { name: '25.0', selected: true},
                { name: '24.1', selected: false},
                { name: '24.0', selected: false}
            ],

            neverExecutedMethods: true,
            probablyDeadMethods: true,
            bootstrapMethods: false,
            liveMethods: false,
            previewRows: 100
        };

        $scope.fullUsageCycle = function() {
            return "14d";
        };

        $scope.recordingStartedAt = function() {
            return '2015-03-17 21:30';
        };

        $scope.bootstrapTime = function() {
            return '60s';
        }

        $scope.selectAll = function (what, selected) {
            angular.forEach($scope.formData[what], function (item) {
                item.selected = selected;
            });
        };

        $scope.dateFormat = 'yyyy-MM-dd HH:mm:ss';
        $scope.showMockup = false;
        $scope.toggleMockupVerb = function () {
            return $scope.showMockup ? "Hide mockup" : "Show mockup"
        };

        $scope.$on('stompDisconnected', function (event, message) {
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

    .run(['RemoteDataService', function (RemoteDataService) {
        RemoteDataService.initSocket();
    }]);
