//noinspection JSUnusedGlobalSymbols
var codekvastApp = angular.module('codekvastApp', [])

    .config(['$locationProvider', function ($locationProvider) {
        $locationProvider.html5Mode(true);
    }])

    .controller('MainCtrl', ['$scope', '$window', function ($scope, $window) {
        $scope.connected = false;
        $scope.haveData = false;
        $scope.maxRows = 100;

        $scope.filterValues = {
            applications: [],
            versions: [],
            tags: []
        };

        $scope.application = undefined;
        $scope.version = undefined;
        $scope.packages = [];
        $scope.package = undefined;
        $scope.signatures = [];
        $scope.timestamp = undefined;

        $scope.sortField = 'invokedAtMillis';
        $scope.reverse = false;

        $scope.socket = {
            client: null,
            stomp: null
        };

        $scope.updateFilterValues = function (data) {
            console.log("Received filter values %o", data);
            $scope.$apply(function () {
                $scope.filterValues = JSON.parse(data.body);
            });
        };

        $scope.updateTimestamps = function (data) {
            console.log("Received timestamps %o", data);
            $scope.$apply(function () {
                $scope.timestamp = JSON.parse(data.body);
            });
        };

        $scope.setSignatures = function (data) {
            console.log("Received signatures");
            $scope.$apply(function() {
                var rsp = JSON.parse(data.body);
                $scope.timestamp = rsp.timestamp;
                $scope.signatures = rsp.signatures;
                $scope.packages = rsp.packages;
                $scope.haveData = true;
            })
        };

        $scope.updateSignatures = function (data) {
            console.log("Received signature updates");
            $scope.$apply(function () {
                var update = JSON.parse(data.body);

                $scope.timestamp = update.timestamp;

                for (var i = 0; i < update.signatures.length; i++) {
                    var s = update.signatures[i];
                    var found = false;

                    for (var j = 0; j < $scope.signatures.length; j++) {
                        if ($scope.signatures[j].name === s.name) {
                            $scope.signatures[j] = s;
                            found = true;
                            console.log("Updated signature %o received", s);
                            break;
                        }
                    }

                    if (!found) {
                        console.log("New signature %o received", s);
                        $scope.signatures.push(s)
                    }
                }

                for (var i = 0; i < update.packages.length; i++) {
                    var p = update.packages[i];
                    if ($scope.packages.indexOf(p) == -1) {
                        $scope.packages.push(p)
                        console.log("Added new package " + p)
                    }
                }
                $scope.haveData = true;
            })
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
                $scope.socket.stomp.subscribe("/app/filterValues", $scope.updateFilterValues);
                $scope.socket.stomp.subscribe("/app/signatures", $scope.setSignatures);
                $scope.socket.stomp.subscribe("/user/queue/filterValues", $scope.updateFilterValues);
                $scope.socket.stomp.subscribe("/user/queue/signatureUpdates", $scope.updateSignatures);
                $scope.socket.stomp.subscribe("/user/queue/timestamps", $scope.updateTimestamps);
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
